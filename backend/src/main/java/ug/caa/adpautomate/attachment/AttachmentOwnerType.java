package ug.caa.adpautomate.attachment;

public enum AttachmentOwnerType {
  NOTAM("notam"),
  PUBLICATION("publication"),
  LIBRARY("library");

  private final String label;

  AttachmentOwnerType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static AttachmentOwnerType fromLabel(String label) {
    for (AttachmentOwnerType type : values()) {
      if (type.label.equalsIgnoreCase(label) || type.name().equalsIgnoreCase(label)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown owner type: " + label);
  }
}
