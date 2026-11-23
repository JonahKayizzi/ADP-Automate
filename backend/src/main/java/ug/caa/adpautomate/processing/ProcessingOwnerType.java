package ug.caa.adpautomate.processing;

public enum ProcessingOwnerType {
  NOTAM("notam"),
  PUBLICATION("publication");

  private final String label;

  ProcessingOwnerType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static ProcessingOwnerType fromLabel(String label) {
    for (ProcessingOwnerType type : values()) {
      if (type.label.equalsIgnoreCase(label) || type.name().equalsIgnoreCase(label)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown owner type: " + label);
  }
}
