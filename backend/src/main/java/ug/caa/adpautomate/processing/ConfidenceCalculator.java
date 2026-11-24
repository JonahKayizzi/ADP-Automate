package ug.caa.adpautomate.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Confidence (%) = (checks scored 100%) / (checks scored 100% or 0%) × 100.
 * Blank and N/A are excluded. Subject to AIM Entebbe confirmation as a quality metric.
 */
final class ConfidenceCalculator {

  private ConfidenceCalculator() {
  }

  record Result(int percent, int scored100, int scoredTotal, int naOrBlank) {
  }

  static Result calculate(Map<String, Object> payload) {
    if (payload == null) {
      return new Result(0, 0, 0, 0);
    }
    List<String> scores = new ArrayList<>();
    for (String key : List.of("accuracy", "resolution", "traceability", "timeliness", "format")) {
      scores.add(stringVal(payload.get(key)));
    }
    Object completeness = payload.get("completeness");
    if (completeness instanceof List<?> rows) {
      for (Object row : rows) {
        if (row instanceof Map<?, ?> map) {
          scores.add(stringVal(map.get("value")));
        }
      }
    }
    if (Boolean.TRUE.equals(payload.get("aixmIncluded"))) {
      Object aixm = payload.get("aixmChecklist");
      if (aixm instanceof List<?> rows) {
        for (Object row : rows) {
          if (row instanceof Map<?, ?> map) {
            scores.add(stringVal(map.get("value")));
          }
        }
      }
    }

    int scored100 = 0;
    int scoredTotal = 0;
    int naOrBlank = 0;
    for (String raw : scores) {
      String normalized = normalizeScore(raw);
      if (normalized == null) {
        naOrBlank++;
        continue;
      }
      scoredTotal++;
      if ("100%".equals(normalized)) {
        scored100++;
      }
    }
    int percent = scoredTotal == 0 ? 0 : (int) Math.round((scored100 * 100.0) / scoredTotal);
    return new Result(percent, scored100, scoredTotal, naOrBlank);
  }

  static String formatLevel(Result result) {
    return result.percent() + "%";
  }

  /**
   * Maps form values to 100% / 0%, or null when excluded (blank / N/A).
   */
  static String normalizeScore(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    if ("N/A".equalsIgnoreCase(trimmed) || "NA".equalsIgnoreCase(trimmed)) {
      return null;
    }
    if ("100%".equals(trimmed) || "Yes".equalsIgnoreCase(trimmed)) {
      return "100%";
    }
    if ("0%".equals(trimmed) || "No".equalsIgnoreCase(trimmed)) {
      return "0%";
    }
    return null;
  }

  private static String stringVal(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
