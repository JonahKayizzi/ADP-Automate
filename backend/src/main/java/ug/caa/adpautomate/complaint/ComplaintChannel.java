package ug.caa.adpautomate.complaint;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ComplaintChannel {
  EMAIL("Email"),
  PHONE("Phone"),
  IN_PERSON("In person"),
  LETTER("Letter"),
  FEEDBACK_FORM("Feedback form");

  private final String label;

  ComplaintChannel(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  public static ComplaintChannel fromLabel(String label) {
    for (ComplaintChannel value : values()) {
      if (value.label.equalsIgnoreCase(label) || value.name().equalsIgnoreCase(label)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown channel: " + label);
  }
}
