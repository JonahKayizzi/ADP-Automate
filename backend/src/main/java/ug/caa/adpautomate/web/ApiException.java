package ug.caa.adpautomate.web;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final String type;
  private final String field;

  public ApiException(HttpStatus status, String type, String detail) {
    this(status, type, detail, null);
  }

  public ApiException(HttpStatus status, String type, String detail, String field) {
    super(detail);
    this.status = status;
    this.type = type;
    this.field = field;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getType() {
    return type;
  }

  public String getField() {
    return field;
  }
}
