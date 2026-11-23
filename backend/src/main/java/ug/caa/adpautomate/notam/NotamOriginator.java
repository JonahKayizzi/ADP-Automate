package ug.caa.adpautomate.notam;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NotamOriginator {
  ENTEBBE_ATC("Entebbe ATC"),
  AERODROME_OPERATIONS("Aerodrome Operations"),
  CNS_NAVAIDS("CNS / Navaids"),
  MET_DEPARTMENT("MET Department"),
  AIM_ENTEBBE("AIM Entebbe"),
  AIRSPACE_ATM_PLANNING("Airspace & ATM Planning"),
  AERODROME_ENGINEERING("Aerodrome Engineering"),
  UETCL("UETCL"),
  OTHER("Other");

  private final String label;

  NotamOriginator(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  public static NotamOriginator fromLabel(String label) {
    if (label == null) {
      throw new IllegalArgumentException("Originator is required");
    }
    for (NotamOriginator value : values()) {
      if (value.label.equalsIgnoreCase(label.trim())) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown originator: " + label);
  }
}
