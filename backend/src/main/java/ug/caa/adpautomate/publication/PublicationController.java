package ug.caa.adpautomate.publication;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ug.caa.adpautomate.publication.dto.PublicationDtos.AiracOption;
import ug.caa.adpautomate.publication.dto.PublicationDtos.PublicationResponse;
import ug.caa.adpautomate.publication.dto.PublicationDtos.UpsertPublicationRequest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/publications")
public class PublicationController {

  private final PublicationService publicationService;

  public PublicationController(PublicationService publicationService) {
    this.publicationService = publicationService;
  }

  @GetMapping
  public List<PublicationResponse> list() {
    return publicationService.list();
  }

  @GetMapping("/types")
  public List<String> types() {
    return Arrays.stream(PublicationType.values()).map(PublicationType::getLabel).toList();
  }

  @GetMapping("/statuses")
  public List<String> statuses() {
    return Arrays.stream(PublicationStatus.values()).map(PublicationStatus::getLabel).toList();
  }

  @GetMapping("/airac-options")
  public List<AiracOption> airacOptions() {
    return publicationService.airacOptions();
  }

  @GetMapping("/{id}")
  public PublicationResponse get(@PathVariable UUID id) {
    return publicationService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PublicationResponse create(@Valid @RequestBody UpsertPublicationRequest request) {
    return publicationService.create(request);
  }

  @PutMapping("/{id}")
  public PublicationResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody UpsertPublicationRequest request
  ) {
    return publicationService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    publicationService.delete(id);
  }
}
