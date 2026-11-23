package ug.caa.adpautomate.notam;

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
import ug.caa.adpautomate.notam.dto.NotamDtos.NotamResponse;
import ug.caa.adpautomate.notam.dto.NotamDtos.UpsertNotamRequest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notams")
public class NotamController {

  private final NotamService notamService;

  public NotamController(NotamService notamService) {
    this.notamService = notamService;
  }

  @GetMapping
  public List<NotamResponse> list() {
    return notamService.list();
  }

  @GetMapping("/originators")
  public List<String> originators() {
    return Arrays.stream(NotamOriginator.values()).map(NotamOriginator::getLabel).toList();
  }

  @GetMapping("/{id}")
  public NotamResponse get(@PathVariable UUID id) {
    return notamService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public NotamResponse create(@Valid @RequestBody UpsertNotamRequest request) {
    return notamService.create(request);
  }

  @PutMapping("/{id}")
  public NotamResponse update(@PathVariable UUID id, @Valid @RequestBody UpsertNotamRequest request) {
    return notamService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    notamService.delete(id);
  }
}
