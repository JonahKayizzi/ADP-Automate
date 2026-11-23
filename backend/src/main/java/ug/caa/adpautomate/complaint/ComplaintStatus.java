package ug.caa.adpautomate.complaint;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ComplaintStatus {
  OPEN("Open"),
  CLOSED("Closed");

  private final String label;

  ComplaintStatus(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  public static ComplaintStatus fromLabel(String label) {
    for (ComplaintStatus value : values()) {
      if (value.label.equalsIgnoreCase(label) || value.name().equalsIgnoreCase(label)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown status: " + label);
  }
}
