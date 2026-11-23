package ug.caa.adpautomate.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    problem.setTitle(ex.getStatus().getReasonPhrase());
    problem.setType(URI.create("https://aisdoc.caa.internal/problems/" + ex.getType()));
    if (ex.getField() != null) {
      problem.setProperty("errors", List.of(Map.of("field", ex.getField(), "message", ex.getMessage())));
    }
    return ResponseEntity.status(ex.getStatus()).body(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    problem.setTitle("Validation failed");
    problem.setType(URI.create("https://aisdoc.caa.internal/problems/validation"));
    List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(this::toError)
        .toList();
    problem.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problem);
  }

  private Map<String, String> toError(FieldError error) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("field", error.getField());
    map.put("message", error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage());
    return map;
  }
}
