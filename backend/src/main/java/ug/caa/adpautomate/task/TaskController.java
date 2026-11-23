package ug.caa.adpautomate.task;

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
import ug.caa.adpautomate.task.dto.TaskDtos.AssigneeOption;
import ug.caa.adpautomate.task.dto.TaskDtos.NotifyRequest;
import ug.caa.adpautomate.task.dto.TaskDtos.TaskResponse;
import ug.caa.adpautomate.task.dto.TaskDtos.UpsertTaskRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping
  public List<TaskResponse> list(@RequestParam(required = false, defaultValue = "Open") String filter) {
    return taskService.list(filter);
  }

  @GetMapping("/assignees")
  public List<AssigneeOption> assignees() {
    return taskService.assignees();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TaskResponse create(@Valid @RequestBody UpsertTaskRequest request) {
    return taskService.create(request);
  }

  @PutMapping("/{id}")
  public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody UpsertTaskRequest request) {
    return taskService.update(id, request);
  }

  @PostMapping("/{id}/complete")
  public TaskResponse complete(@PathVariable UUID id) {
    return taskService.complete(id);
  }

  @PostMapping("/{id}/notify")
  public TaskResponse notify(@PathVariable UUID id, @Valid @RequestBody NotifyRequest request) {
    return taskService.notify(id, request);
  }

  @PostMapping("/mark-seen")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markSeen() {
    taskService.markSeenForCurrentUser();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    taskService.delete(id);
  }
}
