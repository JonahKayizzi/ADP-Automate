package ug.caa.adpautomate.publication;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PublicationType {
  AIRAC_AIP_AMDT("AIRAC AIP AMDT", true),
  AIP_AMDT("AIP AMDT", false),
  AIRAC_AIP_SUP("AIRAC AIP SUP", true),
  AIP_SUP("AIP SUP", false),
  AIC("AIC", false);

  private final String label;
  private final boolean airac;

  PublicationType(String label, boolean airac) {
    this.label = label;
    this.airac = airac;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  public boolean isAirac() {
    return airac;
  }

  public static PublicationType fromLabel(String label) {
    if (label == null) {
      throw new IllegalArgumentException("Type is required");
    }
    for (PublicationType value : values()) {
      if (value.label.equalsIgnoreCase(label.trim())) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown publication type: " + label);
  }
}
