package ug.caa.adpautomate.library;

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
import ug.caa.adpautomate.library.dto.LibraryDtos.LibraryDocumentResponse;
import ug.caa.adpautomate.library.dto.LibraryDtos.UpsertLibraryDocumentRequest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/library")
public class LibraryDocumentController {

  private final LibraryDocumentService libraryDocumentService;

  public LibraryDocumentController(LibraryDocumentService libraryDocumentService) {
    this.libraryDocumentService = libraryDocumentService;
  }

  @GetMapping
  public List<LibraryDocumentResponse> list() {
    return libraryDocumentService.list();
  }

  @GetMapping("/types")
  public List<String> types() {
    return Arrays.stream(LibraryDocType.values()).map(LibraryDocType::getLabel).toList();
  }

  @GetMapping("/{id}")
  public LibraryDocumentResponse get(@PathVariable UUID id) {
    return libraryDocumentService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public LibraryDocumentResponse create(@Valid @RequestBody UpsertLibraryDocumentRequest request) {
    return libraryDocumentService.create(request);
  }

  @PutMapping("/{id}")
  public LibraryDocumentResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody UpsertLibraryDocumentRequest request
  ) {
    return libraryDocumentService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    libraryDocumentService.delete(id);
  }
}
