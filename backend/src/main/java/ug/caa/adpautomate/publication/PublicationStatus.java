package ug.caa.adpautomate.publication;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PublicationStatus {
  DRAFT("Draft"),
  DISTRIBUTED("Distributed"),
  CANCELLED("Cancelled");

  private final String label;

  PublicationStatus(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  public static PublicationStatus fromLabel(String label) {
    if (label == null || label.isBlank()) {
      return DRAFT;
    }
    for (PublicationStatus value : values()) {
      if (value.label.equalsIgnoreCase(label.trim())) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown status: " + label);
  }
}
