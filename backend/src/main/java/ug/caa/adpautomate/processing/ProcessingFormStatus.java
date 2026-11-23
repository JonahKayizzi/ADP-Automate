package ug.caa.adpautomate.processing;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProcessingFormStatus {
  NOT_STARTED("Not started"),
  IN_PROGRESS("In progress"),
  SIGNED_OFF("Signed off");

  private final String label;

  ProcessingFormStatus(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }
}
