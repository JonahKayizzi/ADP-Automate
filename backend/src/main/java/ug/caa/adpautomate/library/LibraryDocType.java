package ug.caa.adpautomate.library;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LibraryDocType {
  ICAO_ANNEX("ICAO Annex"),
  ICAO_DOC("ICAO Doc"),
  EAIP("eAIP"),
  UCAA_REGULATION("UCAA Regulation"),
  UCAA_MANUAL_PROCEDURE("UCAA Manual / Procedure"),
  OTHER("Other");

  private final String label;

  LibraryDocType(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  public static LibraryDocType fromLabel(String label) {
    if (label == null) {
      throw new IllegalArgumentException("Document type is required");
    }
    for (LibraryDocType value : values()) {
      if (value.label.equalsIgnoreCase(label.trim())) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown document type: " + label);
  }
}
