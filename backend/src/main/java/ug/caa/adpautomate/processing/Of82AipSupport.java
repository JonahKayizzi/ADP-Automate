package ug.caa.adpautomate.processing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * AIP (and AIRAC AIP AMDT) OF/82 rules: attachment-driven completeness and promulgation gate.
 */
final class Of82AipSupport {

  static final String ITEM_SOURCE = "All source data received";
  static final String ITEM_COORDS = "Coordinates verified";
  static final String ITEM_DATES = "Effective dates confirmed";
  static final String ITEM_LAYOUT = "Layout / format reviewed";
  static final String ITEM_PROMULGATION = "Signed promulgation form";
  static final String ITEM_SAFETY = "Safety assessment if required";

  /** Removed from AIP completeness; dropped on normalize. */
  static final String REMOVED_ITEM_METADATA = "Meta data for AIP";
  static final String REMOVED_ITEM_REGULATOR = "Approval from the regulator if required";

  private Of82AipSupport() {
  }

  static boolean isAipRecord(String recordKind) {
    if (recordKind == null || recordKind.isBlank()) {
      return false;
    }
    String upper = recordKind.toUpperCase(Locale.ROOT);
    return upper.contains("AMDT") || (upper.contains("AIP") && !upper.contains("SUP"));
  }

  @SuppressWarnings("unchecked")
  static void applyAutoCompleteness(Map<String, Object> payload, Set<String> attachedSlots) {
    if (payload == null) {
      return;
    }
    Object raw = payload.get("completeness");
    if (!(raw instanceof List<?> list)) {
      return;
    }
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Object row : list) {
      if (row instanceof Map<?, ?> map) {
        String item = String.valueOf(map.get("item"));
        if (REMOVED_ITEM_METADATA.equals(item) || REMOVED_ITEM_REGULATOR.equals(item)) {
          continue;
        }
        rows.add(new LinkedHashMap<>((Map<String, Object>) map));
      }
    }
    setIfAttached(rows, ITEM_SOURCE, attachedSlots.contains("data_collection"));
    setIfAttached(rows, ITEM_COORDS, attachedSlots.contains("coordinate_verification"));
    setIfAttached(rows, ITEM_PROMULGATION, attachedSlots.contains("promulgation"));
    setIfAttached(rows, ITEM_SAFETY, attachedSlots.contains("safety_assessment"));
    boolean metaForm = attachedSlots.contains("metadata_form");
    setIfAttached(rows, ITEM_DATES, metaForm);
    setIfAttached(rows, ITEM_LAYOUT, metaForm);
    payload.put("completeness", rows);
  }

  private static void setIfAttached(List<Map<String, Object>> rows, String item, boolean attached) {
    if (attached) {
      setValue(rows, item, "100%");
    }
  }

  private static void setValue(List<Map<String, Object>> rows, String item, String value) {
    for (Map<String, Object> row : rows) {
      if (item.equals(String.valueOf(row.get("item")))) {
        row.put("value", value);
        return;
      }
    }
  }
}
