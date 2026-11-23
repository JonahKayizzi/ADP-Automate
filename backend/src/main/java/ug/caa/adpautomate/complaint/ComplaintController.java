package ug.caa.adpautomate.complaint;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ug.caa.adpautomate.complaint.dto.ComplaintDtos.ComplaintResponse;
import ug.caa.adpautomate.complaint.dto.ComplaintDtos.UpsertComplaintRequest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

  private final ComplaintService complaintService;

  public ComplaintController(ComplaintService complaintService) {
    this.complaintService = complaintService;
  }

  @GetMapping
  public List<ComplaintResponse> list(@RequestParam(required = false, defaultValue = "Open") String status) {
    return complaintService.list(status);
  }

  @GetMapping("/channels")
  public List<String> channels() {
    return Arrays.stream(ComplaintChannel.values()).map(ComplaintChannel::getLabel).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ComplaintResponse create(@Valid @RequestBody UpsertComplaintRequest request) {
    return complaintService.create(request);
  }

  @PutMapping("/{id}")
  public ComplaintResponse update(@PathVariable UUID id, @Valid @RequestBody UpsertComplaintRequest request) {
    return complaintService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    complaintService.delete(id);
  }
}
