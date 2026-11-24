package ug.caa.adpautomate.attachment;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ug.caa.adpautomate.attachment.AttachmentService.LoadedAttachment;
import ug.caa.adpautomate.attachment.dto.AttachmentDtos.AttachmentResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

  private final AttachmentService attachmentService;

  public AttachmentController(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  @GetMapping
  public List<AttachmentResponse> list(
      @RequestParam String ownerType,
      @RequestParam UUID ownerId
  ) {
    return attachmentService.list(ownerType, ownerId);
  }

  @GetMapping("/history")
  public List<AttachmentResponse> history(
      @RequestParam String ownerType,
      @RequestParam UUID ownerId
  ) {
    return attachmentService.listHistory(ownerType, ownerId);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentResponse upload(
      @RequestParam String ownerType,
      @RequestParam UUID ownerId,
      @RequestParam(required = false) String slot,
      @RequestParam("file") MultipartFile file
  ) {
    return attachmentService.upload(ownerType, ownerId, slot, file);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> download(@PathVariable UUID id) {
    LoadedAttachment loaded = attachmentService.downloadAny(id);
    Attachment attachment = loaded.attachment();
    ContentDisposition disposition = ContentDisposition.attachment()
        .filename(attachment.getOriginalFilename(), StandardCharsets.UTF_8)
        .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(MediaType.parseMediaType(attachment.getContentType()))
        .contentLength(attachment.getSizeBytes())
        .body(loaded.resource());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    attachmentService.delete(id);
  }
}
