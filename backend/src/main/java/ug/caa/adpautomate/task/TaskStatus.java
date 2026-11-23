package ug.caa.adpautomate.task;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {
  TO_DO("To do"),
  IN_PROGRESS("In progress"),
  DONE("Done");

  private final String label;

  TaskStatus(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  public static TaskStatus fromLabel(String label) {
    for (TaskStatus value : values()) {
      if (value.label.equalsIgnoreCase(label) || value.name().equalsIgnoreCase(label)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown status: " + label);
  }
}
