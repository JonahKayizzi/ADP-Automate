package ug.caa.adpautomate.task;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NotifyStatus {
  PENDING("pending"),
  EMAILED("emailed"),
  SKIPPED("skipped"),
  NOT_NEEDED("not-needed");

  private final String label;

  NotifyStatus(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  public static NotifyStatus fromLabel(String label) {
    for (NotifyStatus value : values()) {
      if (value.label.equalsIgnoreCase(label) || value.name().equalsIgnoreCase(label)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown notify status: " + label);
  }
}
