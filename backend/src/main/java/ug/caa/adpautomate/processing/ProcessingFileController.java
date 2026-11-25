package ug.caa.adpautomate.processing;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.FormDetail;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.FormRevisionResponse;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.ProcessingFileResponse;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.SaveFormRequest;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.SignRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/processing-files")
public class ProcessingFileController {

  private final ProcessingFileService processingFileService;

  public ProcessingFileController(ProcessingFileService processingFileService) {
    this.processingFileService = processingFileService;
  }

  @GetMapping
  public ProcessingFileResponse getOrCreate(
      @RequestParam String ownerType,
      @RequestParam UUID ownerId
  ) {
    return processingFileService.getOrCreate(ownerType, ownerId);
  }

  @GetMapping("/{fileId}/forms/{formId}")
  public FormDetail getForm(@PathVariable UUID fileId, @PathVariable UUID formId) {
    return processingFileService.getForm(fileId, formId);
  }

  @PutMapping("/{fileId}/forms/{formId}")
  public FormDetail saveForm(
      @PathVariable UUID fileId,
      @PathVariable UUID formId,
      @Valid @RequestBody SaveFormRequest request
  ) {
    return processingFileService.saveForm(fileId, formId, request);
  }

  @PostMapping("/{fileId}/forms/{formId}/signatures/{slotKey}")
  public FormDetail sign(
      @PathVariable UUID fileId,
      @PathVariable UUID formId,
      @PathVariable String slotKey,
      @RequestBody(required = false) SignRequest request
  ) {
    return processingFileService.sign(fileId, formId, slotKey, request);
  }

  @PostMapping("/{fileId}/forms/{formId}/revise")
  public FormDetail revise(@PathVariable UUID fileId, @PathVariable UUID formId) {
    return processingFileService.revise(fileId, formId);
  }

  @PostMapping("/{fileId}/forms/{formId}/clear-signatures")
  public FormDetail clearSignatures(@PathVariable UUID fileId, @PathVariable UUID formId) {
    return processingFileService.clearSignatures(fileId, formId);
  }

  @GetMapping("/{fileId}/forms/{formId}/revisions")
  public List<FormRevisionResponse> listRevisions(
      @PathVariable UUID fileId,
      @PathVariable UUID formId
  ) {
    return processingFileService.listRevisions(fileId, formId);
  }

  @GetMapping("/{fileId}/forms/{formId}/export.pdf")
  public ResponseEntity<byte[]> exportPdf(@PathVariable UUID fileId, @PathVariable UUID formId) {
    byte[] pdf = processingFileService.exportOf82Pdf(fileId, formId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"OF82-raw-data-quality-check.pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }
}
