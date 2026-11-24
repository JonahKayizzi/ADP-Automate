package ug.caa.adpautomate.processing;

import org.springframework.http.HttpStatus;
import ug.caa.adpautomate.web.ApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enforces required fields, signature order and four-eyes rules before a slot is signed.
 */
final class SigningRules {

  private SigningRules() {
  }

  static void validate(
      String formCode,
      Map<String, Object> payload,
      String slotKey,
      List<ProcessingSignature> slots,
      String actorUsername
  ) {
    requireFields(formCode, payload, slotKey);
    requireOrder(formCode, slotKey, slots);
    requireFourEyes(formCode, slotKey, slots, actorUsername);
  }

  static void requireFields(String formCode, Map<String, Object> payload, String slotKey) {
    List<String> missing = new ArrayList<>();
    Map<String, Object> data = payload == null ? Map.of() : payload;

    if (formCode != null && formCode.endsWith("/OF/82")) {
      missing.addAll(missingKeys(data, List.of(
          "approver", "approvalDate", "accuracy", "resolution",
          "integrity", "traceability", "timeliness", "format"
      )));
    } else if (formCode != null && formCode.endsWith("/OF/83d")) {
      missing.addAll(missingKeys(data, List.of(
          "affectedSection", "approver", "approvalDate", "accuracy", "resolution",
          "integrity", "traceability", "timeliness", "format"
      )));
    } else if (formCode != null && formCode.endsWith("/OF/83b")) {
      missing.addAll(missingKeys(data, List.of(
          "personResponsible", "datePerformed", "standardsApplied"
      )));
    } else if (formCode != null && formCode.endsWith("/OF/83a")) {
      missing.addAll(missingKeys(data, List.of(
          "adqCompliance", "personResponsible", "operation",
          "datePerformed", "standardsApplied", "effectiveDate"
      )));
    } else if (formCode != null && formCode.endsWith("/OF/83c")) {
      Map<String, Object> update = part1Row(data, "Update");
      if (isBlank(update.get("description"))) {
        missing.add("Update description");
      }
      if (isBlank(update.get("completed"))) {
        missing.add("Update successfully completed");
      }
      if ("firstCheck".equals(slotKey)) {
        Map<String, Object> row = part1Row(data, "1st Check");
        if (isBlank(row.get("completed"))) {
          missing.add("1st Check successfully completed");
        }
      }
      if ("secondCheck".equals(slotKey)) {
        Map<String, Object> row = part1Row(data, "2nd Check");
        if (isBlank(row.get("completed"))) {
          missing.add("2nd Check successfully completed");
        }
      }
    }

    if (!missing.isEmpty()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "Fill in " + String.join(", ", missing) + " before signing."
      );
    }
  }

  static void requireOrder(String formCode, String slotKey, List<ProcessingSignature> slots) {
    Map<String, ProcessingSignature> byKey = index(slots);
    if (isConfirmSlot(slotKey)) {
      String checkKey = checkSlotFor(slotKey);
      ProcessingSignature check = byKey.get(checkKey);
      if (check == null || !check.isSigned()) {
        throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "validation",
            "The check signature must be completed before confirmation."
        );
      }
    }
    if ("firstCheck".equals(slotKey)) {
      ProcessingSignature update = byKey.get("update");
      if (update == null || !update.isSigned()) {
        throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "validation",
            "Update must be signed before the 1st check."
        );
      }
    }
    if ("secondCheck".equals(slotKey)) {
      ProcessingSignature first = byKey.get("firstCheck");
      if (first == null || !first.isSigned()) {
        throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "validation",
            "1st check must be signed before the 2nd check."
        );
      }
    }
  }

  static void requireFourEyes(
      String formCode,
      String slotKey,
      List<ProcessingSignature> slots,
      String actorUsername
  ) {
    if (actorUsername == null || actorUsername.isBlank()) {
      return;
    }
    Map<String, ProcessingSignature> byKey = index(slots);
    if (isConfirmSlot(slotKey)) {
      ProcessingSignature check = byKey.get(checkSlotFor(slotKey));
      if (check != null && check.isSigned()
          && actorUsername.equalsIgnoreCase(check.getSignedByAccount())) {
        throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "validation",
            "The confirmer must be a different person from the checker."
        );
      }
    }
    if ("secondCheck".equals(slotKey)) {
      ProcessingSignature first = byKey.get("firstCheck");
      if (first != null && first.isSigned()
          && actorUsername.equalsIgnoreCase(first.getSignedByAccount())) {
        throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "validation",
            "The 2nd check must be a different person from the 1st check."
        );
      }
    }
  }

  private static boolean isConfirmSlot(String slotKey) {
    return "qualityConfirmedBy".equals(slotKey) || "productConfirmedBy".equals(slotKey);
  }

  private static String checkSlotFor(String confirmSlot) {
    if ("qualityConfirmedBy".equals(confirmSlot)) {
      return "qualityCheckedBy";
    }
    if ("productConfirmedBy".equals(confirmSlot)) {
      return "productCheckedBy";
    }
    return confirmSlot;
  }

  private static Map<String, ProcessingSignature> index(List<ProcessingSignature> slots) {
    return slots.stream().collect(Collectors.toMap(
        ProcessingSignature::getSlotKey,
        s -> s,
        (a, b) -> a
    ));
  }

  private static List<String> missingKeys(Map<String, Object> data, List<String> keys) {
    List<String> missing = new ArrayList<>();
    for (String key : keys) {
      if (isBlank(data.get(key))) {
        missing.add(labelFor(key));
      }
    }
    return missing;
  }

  private static String labelFor(String key) {
    return switch (key) {
      case "approver" -> "approver";
      case "approvalDate" -> "date of approval";
      case "accuracy" -> "accuracy";
      case "resolution" -> "resolution";
      case "integrity" -> "integrity";
      case "traceability" -> "traceability";
      case "timeliness" -> "timeliness";
      case "format" -> "format";
      case "affectedSection" -> "affected section";
      case "personResponsible" -> "person responsible";
      case "datePerformed" -> "date performed";
      case "standardsApplied" -> "standards applied";
      case "adqCompliance" -> "ADQ compliance";
      case "operation" -> "operation performed";
      case "effectiveDate" -> "effective date";
      default -> key;
    };
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> part1Row(Map<String, Object> payload, String label) {
    Object part1 = payload.get("part1");
    if (!(part1 instanceof List<?> rows)) {
      return Map.of();
    }
    for (Object row : rows) {
      if (row instanceof Map<?, ?> map && label.equals(String.valueOf(map.get("label")))) {
        return (Map<String, Object>) map;
      }
    }
    return Map.of();
  }

  private static boolean isBlank(Object value) {
    return value == null || String.valueOf(value).isBlank();
  }
}
