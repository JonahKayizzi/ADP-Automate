package ug.caa.adpautomate.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.account.AccountRole;
import ug.caa.adpautomate.attachment.Attachment;
import ug.caa.adpautomate.attachment.AttachmentOwnerType;
import ug.caa.adpautomate.attachment.AttachmentRepository;
import ug.caa.adpautomate.audit.AuditService;
import ug.caa.adpautomate.auth.AccountPrincipal;
import ug.caa.adpautomate.auth.AuthService;
import ug.caa.adpautomate.notam.Notam;
import ug.caa.adpautomate.notam.NotamRepository;
import ug.caa.adpautomate.processing.ProcessingFormCatalog.FormDef;
import ug.caa.adpautomate.processing.ProcessingFormCatalog.SlotDef;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.FormDetail;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.FormRevisionResponse;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.FormSummary;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.ProcessingFileResponse;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.SaveFormRequest;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.SignRequest;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.SignatureResponse;
import ug.caa.adpautomate.publication.Publication;
import ug.caa.adpautomate.publication.PublicationRepository;
import ug.caa.adpautomate.publication.PublicationStatus;
import ug.caa.adpautomate.web.ApiException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProcessingFileService {

  private final ProcessingFileRepository files;
  private final ProcessingFormRepository forms;
  private final ProcessingSignatureRepository signatures;
  private final ProcessingFormRevisionRepository revisions;
  private final NotamRepository notams;
  private final PublicationRepository publications;
  private final AttachmentRepository attachments;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public ProcessingFileService(
      ProcessingFileRepository files,
      ProcessingFormRepository forms,
      ProcessingSignatureRepository signatures,
      ProcessingFormRevisionRepository revisions,
      NotamRepository notams,
      PublicationRepository publications,
      AttachmentRepository attachments,
      AuditService auditService,
      ObjectMapper objectMapper
  ) {
    this.files = files;
    this.forms = forms;
    this.signatures = signatures;
    this.revisions = revisions;
    this.notams = notams;
    this.publications = publications;
    this.attachments = attachments;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public ProcessingFileResponse getOrCreate(String ownerTypeLabel, UUID ownerId) {
    ProcessingOwnerType ownerType = parseOwner(ownerTypeLabel);
    String recordKind = resolveRecordKind(ownerType, ownerId);
    ProcessingFile file = files.findByOwnerTypeAndOwnerIdAndRetiredFalse(ownerType, ownerId)
        .orElseGet(() -> createFile(ownerType, ownerId, recordKind));
    return toFileResponse(file);
  }

  @Transactional(readOnly = true)
  public FormDetail getForm(UUID fileId, UUID formId) {
    ProcessingFile file = requireFile(fileId);
    ProcessingForm form = requireForm(file.getId(), formId);
    return toFormDetail(form, file);
  }

  @Transactional
  public FormDetail saveForm(UUID fileId, UUID formId, SaveFormRequest request) {
    AccountPrincipal actor = requireEditor();
    ProcessingFile file = requireFile(fileId);
    ProcessingForm form = requireForm(file.getId(), formId);
    if (form.getStatus() == ProcessingFormStatus.SIGNED_OFF) {
      throw new ApiException(HttpStatus.CONFLICT, "locked", "This form is signed off. Revise it before editing.");
    }
    if (request == null || request.payload() == null) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Payload is required.", "payload");
    }

    Map<String, Object> payload =
        ProcessingFormCatalog.normalizePayload(form.getFormCode(), request.payload(), file.getRecordKind());
    applyAipOf82Rules(file, form, payload, true);
    applyConfidence(form.getFormCode(), payload);

    Map<String, Object> existing =
        ProcessingFormCatalog.normalizePayload(form.getFormCode(), form.getPayload(), file.getRecordKind());
    applyAipOf82Rules(file, form, existing, false);
    applyConfidence(form.getFormCode(), existing);
    boolean payloadChanged = !payloadEquals(existing, payload);

    boolean hadSignatures = false;
    if (payloadChanged) {
      hadSignatures = clearSignaturesIfPresent(form.getId());
    }

    form.setPayload(payload);
    form.setStatus(ProcessingFormStatus.IN_PROGRESS);
    form.setUpdatedAt(Instant.now());
    form.setUpdatedByAccount(actor.getUsername());
    forms.save(form);
    file.setUpdatedAt(Instant.now());
    files.save(file);
    auditService.record(
        "processing.form.save",
        "processing_form",
        form.getId().toString(),
        Map.of(
            "formCode", form.getFormCode(),
            "signaturesCleared", hadSignatures,
            "payloadChanged", payloadChanged,
            "fileId", file.getId().toString()
        )
    );
    return toFormDetail(form, file);
  }

  @Transactional
  public FormDetail clearSignatures(UUID fileId, UUID formId) {
    AccountPrincipal actor = requireEditor();
    ProcessingFile file = requireFile(fileId);
    ProcessingForm form = requireForm(file.getId(), formId);
    if (form.getStatus() == ProcessingFormStatus.SIGNED_OFF) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "locked",
          "This form is signed off. Use Revise form to reopen it."
      );
    }
    boolean hadSignatures = clearSignaturesIfPresent(form.getId());
    if (!hadSignatures) {
      return toFormDetail(form, file);
    }
    form.setStatus(ProcessingFormStatus.IN_PROGRESS);
    form.setUpdatedAt(Instant.now());
    form.setUpdatedByAccount(actor.getUsername());
    forms.save(form);
    file.setUpdatedAt(Instant.now());
    files.save(file);
    auditService.record(
        "processing.form.clear_signatures",
        "processing_form",
        form.getId().toString(),
        Map.of("formCode", form.getFormCode(), "fileId", file.getId().toString())
    );
    return toFormDetail(form, file);
  }

  @Transactional
  public FormDetail sign(UUID fileId, UUID formId, String slotKey, SignRequest request) {
    AccountPrincipal actor = requireEditor();
    ProcessingFile file = requireFile(fileId);
    ProcessingForm form = requireForm(file.getId(), formId);
    if (form.getStatus() == ProcessingFormStatus.SIGNED_OFF) {
      throw new ApiException(HttpStatus.CONFLICT, "locked", "This form is already signed off.");
    }
    ProcessingSignature slot = signatures.findByFormIdAndSlotKey(form.getId(), slotKey)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "Signature slot not found."));
    if (slot.isSigned()) {
      throw new ApiException(HttpStatus.CONFLICT, "signed", "This signature slot is already signed.");
    }

    Map<String, Object> payload =
        ProcessingFormCatalog.normalizePayload(form.getFormCode(), form.getPayload(), file.getRecordKind());
    applyAipOf82Rules(file, form, payload, true);
    applyConfidence(form.getFormCode(), payload);
    form.setPayload(payload);

    List<ProcessingSignature> existingSlots = signatures.findByFormIdOrderBySlotKeyAsc(form.getId());
    SigningRules.validate(
        form.getFormCode(),
        payload,
        slotKey,
        existingSlots,
        actor.getUsername()
    );

    String name = blankToNull(request == null ? null : request.signerName());
    String designation = blankToNull(request == null ? null : request.designation());
    if (name == null) {
      name = actor.getDisplayName();
    }
    if (designation == null) {
      designation = actor.getDesignation();
    }
    if (name == null || name.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Signer name is required.", "signerName");
    }

    Instant now = Instant.now();
    slot.setSignerName(name.trim());
    slot.setDesignation(designation);
    slot.setSignedAt(now);
    slot.setSignedBy(actor.getId());
    slot.setSignedByAccount(actor.getUsername());
    signatures.save(slot);

    form.setStatus(ProcessingFormStatus.IN_PROGRESS);
    form.setUpdatedAt(now);
    form.setUpdatedByAccount(actor.getUsername());

    List<ProcessingSignature> all = signatures.findByFormIdOrderBySlotKeyAsc(form.getId());
    if (all.stream().allMatch(ProcessingSignature::isSigned)) {
      form.setStatus(ProcessingFormStatus.SIGNED_OFF);
    }
    forms.save(form);
    file.setUpdatedAt(now);
    files.save(file);
    auditService.record(
        "processing.form.sign",
        "processing_form",
        form.getId().toString(),
        Map.of(
            "formCode", form.getFormCode(),
            "slotKey", slotKey,
            "status", form.getStatus().getLabel()
        )
    );
    return toFormDetail(form, file);
  }

  @Transactional
  public FormDetail revise(UUID fileId, UUID formId) {
    AccountPrincipal actor = requireEditor();
    ProcessingFile file = requireFile(fileId);
    ProcessingForm form = requireForm(file.getId(), formId);
    archiveRevision(form, actor.getUsername());
    clearSignatures(form.getId());
    form.setStatus(ProcessingFormStatus.IN_PROGRESS);
    form.setUpdatedAt(Instant.now());
    form.setUpdatedByAccount(actor.getUsername());
    forms.save(form);
    file.setUpdatedAt(Instant.now());
    files.save(file);
    auditService.record(
        "processing.form.revise",
        "processing_form",
        form.getId().toString(),
        Map.of("formCode", form.getFormCode(), "fileId", file.getId().toString())
    );
    return toFormDetail(form);
  }

  @Transactional(readOnly = true)
  public List<FormRevisionResponse> listRevisions(UUID fileId, UUID formId) {
    requireFile(fileId);
    ProcessingForm form = requireForm(fileId, formId);
    return revisions.findByFormIdOrderByRevisionNumberDesc(form.getId()).stream()
        .map(r -> new FormRevisionResponse(
            r.getId(),
            r.getRevisionNumber(),
            r.getFormCode(),
            r.getPayload(),
            r.getSignatures(),
            r.getRevisedAt(),
            r.getRevisedByAccount()
        ))
        .toList();
  }

  private void archiveRevision(ProcessingForm form, String actorUsername) {
    List<ProcessingSignature> slots = signatures.findByFormIdOrderBySlotKeyAsc(form.getId());
    boolean anySigned = slots.stream().anyMatch(ProcessingSignature::isSigned);
    if (!anySigned && form.getStatus() != ProcessingFormStatus.SIGNED_OFF) {
      return;
    }
    int next = revisions.findFirstByFormIdOrderByRevisionNumberDesc(form.getId())
        .map(r -> r.getRevisionNumber() + 1)
        .orElse(1);
    List<Map<String, Object>> sigSnapshot = new ArrayList<>();
    for (ProcessingSignature slot : slots) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("slotKey", slot.getSlotKey());
      row.put("slotLabel", slot.getSlotLabel());
      row.put("signerName", slot.getSignerName());
      row.put("designation", slot.getDesignation());
      row.put("signedAt", slot.getSignedAt() == null ? null : slot.getSignedAt().toString());
      row.put("signedByAccount", slot.getSignedByAccount());
      row.put("signed", slot.isSigned());
      sigSnapshot.add(row);
    }
    ProcessingFormRevision revision = new ProcessingFormRevision();
    revision.setId(UUID.randomUUID());
    revision.setFormId(form.getId());
    revision.setRevisionNumber(next);
    revision.setFormCode(form.getFormCode());
    revision.setPayload(deepCopy(form.getPayload()));
    revision.setSignatures(sigSnapshot);
    revision.setRevisedAt(Instant.now());
    revision.setRevisedByAccount(actorUsername);
    revisions.save(revision);
  }

  @Transactional(readOnly = true)
  public long countNotFullySignedOff() {
    long count = 0;
    for (Notam notam : notams.findByRetiredFalseOrderByIssuedDescCreatedAtDesc()) {
      if (!isFullySignedOff(ProcessingOwnerType.NOTAM, notam.getId())) {
        count++;
      }
    }
    for (Publication publication : publications.findByRetiredFalseOrderByEffectiveDescCreatedAtDesc()) {
      if (publication.getStatus() == PublicationStatus.CANCELLED) {
        continue;
      }
      if (!isFullySignedOff(ProcessingOwnerType.PUBLICATION, publication.getId())) {
        count++;
      }
    }
    return count;
  }

  private boolean isFullySignedOff(ProcessingOwnerType ownerType, UUID ownerId) {
    return files.findByOwnerTypeAndOwnerIdAndRetiredFalse(ownerType, ownerId)
        .map(file -> forms.countByProcessingFileIdAndStatus(file.getId(), ProcessingFormStatus.SIGNED_OFF) == 4)
        .orElse(false);
  }

  private ProcessingFile createFile(ProcessingOwnerType ownerType, UUID ownerId, String recordKind) {
    Instant now = Instant.now();
    ProcessingFile file = new ProcessingFile();
    file.setId(UUID.randomUUID());
    file.setOwnerType(ownerType);
    file.setOwnerId(ownerId);
    file.setRecordKind(recordKind);
    file.setRetired(false);
    file.setCreatedAt(now);
    file.setUpdatedAt(now);
    files.save(file);

    for (FormDef def : ProcessingFormCatalog.forRecordKind(recordKind)) {
      ProcessingForm form = new ProcessingForm();
      form.setId(UUID.randomUUID());
      form.setProcessingFileId(file.getId());
      form.setStepNumber(def.step());
      form.setFormCode(def.code());
      form.setFormTitle(def.title());
      form.setControlledNotice(def.controlledNotice());
      form.setPayload(deepCopy(def.emptyPayload()));
      form.setStatus(ProcessingFormStatus.NOT_STARTED);
      form.setCreatedAt(now);
      form.setUpdatedAt(now);
      forms.save(form);
      for (SlotDef slot : def.slots()) {
        ProcessingSignature signature = new ProcessingSignature();
        signature.setId(UUID.randomUUID());
        signature.setFormId(form.getId());
        signature.setSlotKey(slot.key());
        signature.setSlotLabel(slot.label());
        signatures.save(signature);
      }
    }
    return file;
  }

  private String resolveRecordKind(ProcessingOwnerType ownerType, UUID ownerId) {
    if (ownerType == ProcessingOwnerType.NOTAM) {
      notams.findByIdAndRetiredFalse(ownerId)
          .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "NOTAM not found."));
      return "NOTAM";
    }
    Publication publication = publications.findByIdAndRetiredFalse(ownerId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "Publication not found."));
    if (publication.getStatus() == PublicationStatus.CANCELLED) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Cancelled publications do not carry a processing file.");
    }
    return publication.getType().getLabel();
  }

  private ProcessingFile requireFile(UUID id) {
    return files.findById(id)
        .filter(f -> !f.isRetired())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "Processing file not found."));
  }

  private ProcessingForm requireForm(UUID fileId, UUID formId) {
    return forms.findByIdAndProcessingFileId(formId, fileId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "Processing form not found."));
  }

  private boolean clearSignaturesIfPresent(UUID formId) {
    List<ProcessingSignature> slots = signatures.findByFormIdOrderBySlotKeyAsc(formId);
    boolean any = slots.stream().anyMatch(ProcessingSignature::isSigned);
    if (any) {
      clearSignatures(formId);
    }
    return any;
  }

  private void clearSignatures(UUID formId) {
    List<ProcessingSignature> slots = signatures.findByFormIdOrderBySlotKeyAsc(formId);
    for (ProcessingSignature slot : slots) {
      slot.setSignerName(null);
      slot.setDesignation(null);
      slot.setSignedAt(null);
      slot.setSignedBy(null);
      slot.setSignedByAccount(null);
    }
    signatures.saveAll(slots);
  }

  private ProcessingFileResponse toFileResponse(ProcessingFile file) {
    List<ProcessingForm> formList = forms.findByProcessingFileIdOrderByStepNumberAsc(file.getId());
    List<UUID> formIds = formList.stream().map(ProcessingForm::getId).toList();
    Map<UUID, List<ProcessingSignature>> byForm = signatures.findByFormIdIn(formIds).stream()
        .collect(Collectors.groupingBy(ProcessingSignature::getFormId));

    List<FormSummary> summaries = new ArrayList<>();
    int signedOff = 0;
    for (ProcessingForm form : formList) {
      List<ProcessingSignature> slots = byForm.getOrDefault(form.getId(), List.of());
      int signedCount = (int) slots.stream().filter(ProcessingSignature::isSigned).count();
      if (form.getStatus() == ProcessingFormStatus.SIGNED_OFF) {
        signedOff++;
      }
      summaries.add(new FormSummary(
          form.getId(),
          form.getStepNumber(),
          form.getFormCode(),
          form.getFormTitle(),
          form.getStatus().getLabel(),
          signedCount,
          slots.size()
      ));
    }
    return new ProcessingFileResponse(
        file.getId(),
        file.getOwnerType().getLabel(),
        file.getOwnerId(),
        file.getRecordKind(),
        signedOff,
        formList.size(),
        summaries
    );
  }

  private FormDetail toFormDetail(ProcessingForm form) {
    ProcessingFile file = files.findById(form.getProcessingFileId()).orElse(null);
    return toFormDetail(form, file);
  }

  private FormDetail toFormDetail(ProcessingForm form, ProcessingFile file) {
    List<SignatureResponse> slots = signatures.findByFormIdOrderBySlotKeyAsc(form.getId()).stream()
        .map(s -> new SignatureResponse(
            s.getSlotKey(),
            s.getSlotLabel(),
            s.getSignerName(),
            s.getDesignation(),
            s.getSignedAt(),
            s.getSignedByAccount(),
            s.isSigned()
        ))
        .toList();
    String recordKind = file == null ? null : file.getRecordKind();
    Map<String, Object> payload =
        ProcessingFormCatalog.normalizePayload(form.getFormCode(), form.getPayload(), recordKind);
    if (file != null && form.getFormCode() != null && form.getFormCode().endsWith("/OF/82")) {
      applyAipOf82Rules(file, form, payload, false);
    }
    return new FormDetail(
        form.getId(),
        form.getStepNumber(),
        form.getFormCode(),
        form.getFormTitle(),
        form.getControlledNotice(),
        form.getStatus().getLabel(),
        form.getStatus() == ProcessingFormStatus.SIGNED_OFF,
        payload,
        slots,
        form.getUpdatedAt()
    );
  }

  @Transactional(readOnly = true)
  public byte[] exportOf82Pdf(UUID fileId, UUID formId) {
    ProcessingFile file = requireFile(fileId);
    ProcessingForm form = requireForm(file.getId(), formId);
    if (form.getFormCode() == null || !form.getFormCode().endsWith("/OF/82")) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Only OF/82 forms can be exported as PDF.");
    }
    if (form.getStatus() != ProcessingFormStatus.SIGNED_OFF) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "incomplete",
          "Export is available only after the raw data quality check is signed off."
      );
    }
    FormDetail detail = toFormDetail(form, file);
    try {
      return Of82PdfExporter.export(
          detail.formCode(),
          detail.formTitle(),
          file.getRecordKind(),
          detail.payload(),
          detail.signatures()
      );
    } catch (IOException ex) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "export", "Could not generate the PDF export.");
    }
  }

  private void applyAipOf82Rules(
      ProcessingFile file,
      ProcessingForm form,
      Map<String, Object> payload,
      boolean enforcePromulgation
  ) {
    if (form.getFormCode() == null || !form.getFormCode().endsWith("/OF/82")) {
      return;
    }
    if (!Of82AipSupport.isAipRecord(file.getRecordKind())) {
      return;
    }
    if (enforcePromulgation) {
      requirePromulgationAttached(file);
    }
    Of82AipSupport.applyAutoCompleteness(payload, attachedSlots(file));
  }

  private void requirePromulgationAttached(ProcessingFile file) {
    if (!attachedSlots(file).contains("promulgation")) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "Attach a promulgation form under Production files before updating the raw data quality check."
      );
    }
  }

  private Set<String> attachedSlots(ProcessingFile file) {
    AttachmentOwnerType ownerType = file.getOwnerType() == ProcessingOwnerType.NOTAM
        ? AttachmentOwnerType.NOTAM
        : AttachmentOwnerType.PUBLICATION;
    return attachments
        .findByOwnerTypeAndOwnerIdAndRetiredFalseOrderByCreatedAtDesc(ownerType, file.getOwnerId())
        .stream()
        .map(Attachment::getSlot)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private boolean payloadEquals(Map<String, Object> left, Map<String, Object> right) {
    try {
      return objectMapper.writeValueAsString(left == null ? Map.of() : left)
          .equals(objectMapper.writeValueAsString(right == null ? Map.of() : right));
    } catch (java.io.IOException ex) {
      return false;
    }
  }

  private static void applyConfidence(String formCode, Map<String, Object> payload) {
    if (formCode == null || payload == null) {
      return;
    }
    if (formCode.endsWith("/OF/82")) {
      Object manual = payload.get("confidenceManual");
      if (Boolean.TRUE.equals(manual) || "true".equalsIgnoreCase(String.valueOf(manual))) {
        Object level = payload.get("confidenceLevel");
        if (level == null || String.valueOf(level).isBlank()) {
          payload.put("confidenceLevel", "0%");
        }
        return;
      }
    }
    if (formCode.endsWith("/OF/82") || formCode.endsWith("/OF/83d")) {
      ConfidenceCalculator.Result result = ConfidenceCalculator.calculate(payload);
      payload.put("confidenceLevel", ConfidenceCalculator.formatLevel(result));
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> deepCopy(Map<String, Object> source) {
    try {
      return objectMapper.readValue(objectMapper.writeValueAsBytes(source), LinkedHashMap.class);
    } catch (java.io.IOException ex) {
      return new LinkedHashMap<>(source);
    }
  }

  private ProcessingOwnerType parseOwner(String label) {
    try {
      return ProcessingOwnerType.fromLabel(label);
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Owner type must be notam or publication.", "ownerType");
    }
  }

  private AccountPrincipal requireEditor() {
    AccountPrincipal principal = AuthService.currentPrincipal()
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));
    if (principal.getRole() == AccountRole.VIEWER) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "forbidden",
          "Your account has the Viewer role, so you can read records but not change them."
      );
    }
    return principal;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
