package ug.caa.adpautomate.processing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ProcessingFormCatalog {

  private ProcessingFormCatalog() {
  }

  record SlotDef(String key, String label) {
  }

  record FormDef(
      int step,
      String code,
      String title,
      String controlledNotice,
      List<SlotDef> slots,
      Map<String, Object> emptyPayload
  ) {
  }

  static List<FormDef> forRecordKind(String recordKind) {
    boolean amd = recordKind.toUpperCase().contains("AMDT");
    List<FormDef> forms = new ArrayList<>();
    forms.add(of82(recordKind));
    forms.add(amd ? of83a() : of83b());
    forms.add(of83c());
    forms.add(of83d(recordKind));
    return forms;
  }

  private static FormDef of82(String recordKind) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("findings", "");
    payload.put("approver", "");
    payload.put("approvalDate", "");
    payload.put("accuracy", "");
    payload.put("resolution", "");
    payload.put("traceability", "");
    payload.put("timeliness", "");
    payload.put("format", "");
    payload.put("integrity", "");
    payload.put("dateReceived", "");
    payload.put("publicationDate", "");
    payload.put("fileType", "");
    payload.put("aixmIncluded", false);
    payload.put("completeness", defaultCompleteness(recordKind, false));
    payload.put("aixmChecklist", defaultAixmChecklist());
    payload.put("confidenceLevel", "");
    payload.put("confidenceManual", true);
    payload.put("accuracyComment", "");
    payload.put("resolutionComment", "");
    payload.put("traceabilityComment", "");
    payload.put("timelinessComment", "");
    payload.put("formatComment", "");
    payload.put("integrityComment", "");
    return new FormDef(
        1,
        "UCAA/DANS/AIM/OF/82",
        "Raw data quality check",
        "Controlled document — MANSOPS Part III (Rev 04, 01/08/2023)",
        List.of(
            new SlotDef("qualityCheckedBy", "Raw data quality checked by"),
            new SlotDef("qualityConfirmedBy", "Raw data quality check confirmed by")
        ),
        payload
    );
  }

  /**
   * AIC/NOTAM/SUP metadata (OF/83b) — 14 principal fields from MANSOPS / prototype.
   */
  private static FormDef of83b() {
    return new FormDef(
        2,
        "UCAA/DANS/AIM/OF/83b",
        "AIC/NOTAM/SUP meta data form",
        "Controlled document — MANSOPS Part III (Rev 04, 01/08/2023)",
        List.of(new SlotDef("preparedBy", "Prepared by")),
        emptyOf83b()
    );
  }

  /**
   * AIP amendment metadata (OF/83a) — ADQ / operation fields plus shared quality/tool rows.
   */
  private static FormDef of83a() {
    return new FormDef(
        2,
        "UCAA/DANS/AIM/OF/83a",
        "AIP meta data form",
        "Controlled document — MANSOPS Part III (Rev 04, 01/08/2023)",
        List.of(new SlotDef("preparedBy", "Prepared by")),
        emptyOf83a()
    );
  }

  static Map<String, Object> emptyOf83b() {
    Map<String, Object> payload = new LinkedHashMap<>();
    putBlank(payload,
        "personResponsible",
        "role",
        "datePerformed",
        "standardsApplied",
        "effectiveDateTime",
        "endDateTime",
        "schedule",
        "referenceSystem",
        "methodUsed",
        "qualityAccuracy",
        "qualityResolution",
        "conversionUsed",
        "tool",
        "toolVersion"
    );
    return payload;
  }

  static Map<String, Object> emptyOf83a() {
    Map<String, Object> payload = new LinkedHashMap<>();
    putBlank(payload,
        "adqCompliance",
        "reviewDate",
        "personResponsible",
        "role",
        "operation",
        "datePerformed",
        "standardsApplied",
        "effectiveDate",
        "effectiveTime",
        "referenceSystem",
        "methodUsed",
        "qualityAccuracy",
        "qualityResolution",
        "conversionUsed",
        "tool",
        "toolVersion"
    );
    return payload;
  }

  /**
   * Merge a stored payload onto the current empty template so older forms pick up new keys
   * and obsolete keys (e.g. legacy {@code rows}) are dropped from the working set.
   */
  static Map<String, Object> normalizePayload(String formCode, Map<String, Object> stored) {
    return normalizePayload(formCode, stored, null);
  }

  static Map<String, Object> normalizePayload(String formCode, Map<String, Object> stored, String recordKind) {
    if (formCode != null && formCode.endsWith("/OF/83c")) {
      return normalizeOf83c(stored);
    }
    if (formCode != null && formCode.endsWith("/OF/82")) {
      return normalizeOf82(stored, recordKind);
    }
    Map<String, Object> template = templateFor(formCode);
    if (template == null) {
      return stored == null ? new LinkedHashMap<>() : new LinkedHashMap<>(stored);
    }
    Map<String, Object> merged = new LinkedHashMap<>(template);
    if (stored != null) {
      for (String key : template.keySet()) {
        if (stored.containsKey(key) && stored.get(key) != null) {
          merged.put(key, stored.get(key));
        }
      }
    }
    return merged;
  }

  private static Map<String, Object> emptyOf82Scalars() {
    Map<String, Object> payload = new LinkedHashMap<>();
    putBlank(payload,
        "findings",
        "approver",
        "approvalDate",
        "accuracy",
        "resolution",
        "traceability",
        "timeliness",
        "format",
        "integrity",
        "dateReceived",
        "publicationDate",
        "fileType",
        "confidenceLevel",
        "accuracyComment",
        "resolutionComment",
        "traceabilityComment",
        "timelinessComment",
        "formatComment",
        "integrityComment"
    );
    payload.put("aixmIncluded", false);
    payload.put("confidenceManual", true);
    payload.put("aixmChecklist", defaultAixmChecklist());
    return payload;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> normalizeOf82(Map<String, Object> stored, String recordKind) {
    Map<String, Object> template = emptyOf82Scalars();
    Map<String, Object> merged = new LinkedHashMap<>(template);
    if (stored != null) {
      for (String key : template.keySet()) {
        if ("aixmChecklist".equals(key)) {
          continue;
        }
        if (stored.containsKey(key) && stored.get(key) != null) {
          merged.put(key, stored.get(key));
        }
      }
      if (stored.get("aixmChecklist") instanceof List<?> list && !list.isEmpty()) {
        merged.put("aixmChecklist", list);
      }
    }

    String kind = recordKind == null ? "" : recordKind;
    List<Map<String, Object>> defaultRows = defaultCompleteness(kind, false);
    Map<String, String> storedByItem = new LinkedHashMap<>();
    String storedColumn = null;
    if (stored != null && stored.get("completeness") instanceof List<?> rows) {
      for (Object row : rows) {
        if (row instanceof Map<?, ?> map) {
          String item = String.valueOf(map.get("item"));
          Object value = map.get("value");
          storedByItem.put(item, value == null ? "" : String.valueOf(value));
          if (storedColumn == null && map.get("column") != null) {
            storedColumn = String.valueOf(map.get("column"));
          }
        }
      }
    }
    // Drop removed checklist items from older AIP forms.
    storedByItem.remove("Cross-references checked");
    storedByItem.remove("Cross references checked");
    storedByItem.remove(Of82AipSupport.REMOVED_ITEM_METADATA);
    storedByItem.remove(Of82AipSupport.REMOVED_ITEM_REGULATOR);

    boolean forceAipItems = Of82AipSupport.isAipRecord(kind)
        || "AIP".equalsIgnoreCase(storedColumn)
        || "AIP AMDT".equalsIgnoreCase(storedColumn)
        || storedByItem.containsKey(Of82AipSupport.ITEM_SOURCE)
        || storedByItem.containsKey("Affected AIP section(s)");
    List<Map<String, Object>> completeness;
    if (forceAipItems && Of82AipSupport.isAipRecord(kind)) {
      completeness = defaultCompleteness(kind.isBlank() ? "AIP AMDT" : kind, false);
    } else if (forceAipItems) {
      completeness = defaultCompleteness("AIP AMDT", false);
    } else {
      completeness = defaultRows;
    }
    for (Map<String, Object> row : completeness) {
      String item = String.valueOf(row.get("item"));
      if (storedByItem.containsKey(item)) {
        row.put("value", storedByItem.get(item));
      }
    }
    merged.put("completeness", completeness);
    return merged;
  }

  private static Map<String, Object> templateFor(String formCode) {
    if (formCode == null) {
      return null;
    }
    if (formCode.endsWith("/OF/83b")) {
      return emptyOf83b();
    }
    if (formCode.endsWith("/OF/83a")) {
      return emptyOf83a();
    }
    return null;
  }

  private static void putBlank(Map<String, Object> payload, String... keys) {
    for (String key : keys) {
      payload.put(key, "");
    }
  }

  private static FormDef of83c() {
    return new FormDef(
        3,
        "UCAA/DANS/AIM/OF/83c",
        "Raw data processing form (Part I)",
        "Controlled document — MANSOPS Part III (Rev 04, 01/08/2023)",
        List.of(
            new SlotDef("update", "Update"),
            new SlotDef("firstCheck", "1st Check"),
            new SlotDef("secondCheck", "2nd Check")
        ),
        emptyOf83c()
    );
  }

  static Map<String, Object> emptyOf83c() {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("systems", defaultSystems());
    payload.put("part1", defaultPart1());
    return payload;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> normalizeOf83c(Map<String, Object> stored) {
    Map<String, Object> template = emptyOf83c();
    if (stored == null) {
      return template;
    }
    List<Map<String, Object>> templateSystems = (List<Map<String, Object>>) template.get("systems");
    List<Map<String, Object>> storedSystems = stored.get("systems") instanceof List<?> list
        ? (List<Map<String, Object>>) (List<?>) list
        : List.of();
    List<Map<String, Object>> mergedSystems = new ArrayList<>();
    for (Map<String, Object> systemTemplate : templateSystems) {
      String name = String.valueOf(systemTemplate.get("name"));
      Map<String, Object> storedSystem = storedSystems.stream()
          .filter(s -> name.equals(String.valueOf(s.get("name"))))
          .findFirst()
          .orElse(null);
      Map<String, Object> system = new LinkedHashMap<>();
      system.put("name", name);
      List<Map<String, Object>> templateSteps = (List<Map<String, Object>>) systemTemplate.get("steps");
      List<Map<String, Object>> storedSteps = storedSystem != null && storedSystem.get("steps") instanceof List<?>
          ? (List<Map<String, Object>>) storedSystem.get("steps")
          : List.of();
      List<Map<String, Object>> steps = new ArrayList<>();
      for (Map<String, Object> stepTemplate : templateSteps) {
        String label = String.valueOf(stepTemplate.get("label"));
        String done = "";
        for (Map<String, Object> storedStep : storedSteps) {
          if (label.equals(String.valueOf(storedStep.get("label"))) && storedStep.get("done") != null) {
            done = String.valueOf(storedStep.get("done"));
            break;
          }
        }
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("label", label);
        step.put("done", done);
        steps.add(step);
      }
      system.put("steps", steps);
      mergedSystems.add(system);
    }
    template.put("systems", mergedSystems);

    List<Map<String, Object>> templatePart1 = (List<Map<String, Object>>) template.get("part1");
    List<Map<String, Object>> storedPart1 = stored.get("part1") instanceof List<?> list
        ? (List<Map<String, Object>>) (List<?>) list
        : List.of();
    List<Map<String, Object>> mergedPart1 = new ArrayList<>();
    for (Map<String, Object> rowTemplate : templatePart1) {
      String label = String.valueOf(rowTemplate.get("label"));
      Map<String, Object> storedRow = storedPart1.stream()
          .filter(r -> label.equals(String.valueOf(r.get("label"))))
          .findFirst()
          .orElse(null);
      Map<String, Object> row = new LinkedHashMap<>(rowTemplate);
      if (storedRow != null) {
        for (String key : List.of("description", "completed", "remarks", "date", "by")) {
          if (storedRow.get(key) != null) {
            row.put(key, storedRow.get(key));
          }
        }
      }
      mergedPart1.add(row);
    }
    template.put("part1", mergedPart1);
    return template;
  }

  private static List<Map<String, Object>> defaultSystems() {
    List<Map<String, Object>> systems = new ArrayList<>();
    systems.add(system("AIM Static Database", List.of(
        "Insert data", "Edit data", "1st check", "Confirm 1st check", "2nd check", "Confirm 2nd check"
    )));
    systems.add(system("ACM", List.of(
        "Insert data", "Edit data", "1st check", "Confirm"
    )));
    systems.add(system("Framemaker Text", List.of(
        "Insert / update text", "Edit", "1st check", "Confirm"
    )));
    systems.add(system("Graphics", List.of(
        "Insert / update graphic", "Edit", "1st check", "Confirm"
    )));
    systems.add(system("AIP-AIM Static Database Data Linkage", List.of(
        "Link features", "Verify linkage", "1st check", "Confirm"
    )));
    systems.add(system("Esri Database", List.of(
        "Insert data", "Edit data", "1st check", "Confirm"
    )));
    systems.add(system("AIS Static Database", List.of(
        "Insert data", "Edit data", "1st check", "Confirm"
    )));
    systems.add(system("NOTAM", List.of(
        "Prepare NOTAM promulgation form",
        "1st check promulgation form",
        "Confirm 1st check edits",
        "2nd check promulgation form",
        "Confirm 2nd check edits"
    )));
    systems.add(system("Data sets", List.of(
        "Prepare data set", "Validate data set", "1st check", "Confirm"
    )));
    return systems;
  }

  private static Map<String, Object> system(String name, List<String> stepLabels) {
    Map<String, Object> system = new LinkedHashMap<>();
    system.put("name", name);
    List<Map<String, Object>> steps = new ArrayList<>();
    for (String label : stepLabels) {
      Map<String, Object> step = new LinkedHashMap<>();
      step.put("label", label);
      step.put("done", "");
      steps.add(step);
    }
    system.put("steps", steps);
    return system;
  }

  private static List<Map<String, Object>> defaultPart1() {
    List<Map<String, Object>> part1 = new ArrayList<>();
    for (String label : List.of("Update", "1st Check", "2nd Check")) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("label", label);
      row.put("description", "");
      row.put("completed", "");
      row.put("remarks", "");
      row.put("date", "");
      row.put("by", "");
      part1.add(row);
    }
    return part1;
  }

  private static FormDef of83d(String recordKind) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("affectedSection", "");
    payload.put("approver", "");
    payload.put("approvalDate", "");
    payload.put("accuracy", "");
    payload.put("resolution", "");
    payload.put("traceability", "");
    payload.put("timeliness", "");
    payload.put("format", "");
    payload.put("integrity", "");
    payload.put("finalDraftReviewDate", "");
    payload.put("publicationDate", "");
    payload.put("fileType", "");
    payload.put("applicableRegulations", "");
    payload.put("completeness", defaultCompleteness(recordKind, true));
    payload.put("confidenceLevel", "");
    return new FormDef(
        4,
        "UCAA/DANS/AIM/OF/83d",
        "AIS product quality check (Part II)",
        "Controlled document — MANSOPS Part III (Rev 04, 01/08/2023)",
        List.of(
            new SlotDef("productCheckedBy", "AIS product quality checked by"),
            new SlotDef("productConfirmedBy", "AIS product quality check confirmed by")
        ),
        payload
    );
  }

  private static List<Map<String, Object>> defaultCompleteness(String recordKind, boolean productQc) {
    String column = completenessColumn(recordKind, productQc);
    List<String> items = productQc ? productCompletenessItems(column) : rawCompletenessItems(column);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (String item : items) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("item", item);
      row.put("column", column);
      row.put("value", "");
      rows.add(row);
    }
    return rows;
  }

  private static List<String> rawCompletenessItems(String column) {
    if ("AIC".equals(column)) {
      return List.of(
          "Requirements declaration",
          "Effective date/time",
          "End date/time or PERM",
          "Meta data for AIC",
          "Signed promulgation form",
          "Daily schedule",
          "Approval from regulator",
          "Safety assessment"
      );
    }
    if ("AIP AMDT".equals(column) || "AIP".equals(column)) {
      return List.of(
          "All source data received",
          "Coordinates verified",
          "Effective dates confirmed",
          "Layout / format reviewed",
          "Signed promulgation form",
          "Safety assessment if required"
      );
    }
    return List.of(
        "Requirements declaration",
        "Effective date/time",
        "End date/time",
        "Meta data for AIP SUP/NOTAM",
        "Signed promulgation form",
        "Daily schedule",
        "Approval from regulator",
        "Safety assessment"
    );
  }

  private static List<String> productCompletenessItems(String column) {
    if ("AIC".equals(column)) {
      return List.of(
          "Serial number",
          "Title of AIC",
          "Effective date",
          "End date or PERM",
          "Subject / purpose stated",
          "References complete",
          "Publication date",
          "Distribution list"
      );
    }
    if ("AIP AMDT".equals(column)) {
      return List.of(
          "Serial number",
          "Title of AIP AMDT",
          "Affected section in the AIP",
          "Effective date",
          "Checklist of pages",
          "Publication date",
          "AIRAC ident (if AIRAC)",
          "References / cancelled pages"
      );
    }
    return List.of(
        "Serial number",
        "Distinctive pages",
        "Title of AIP [AIRAC] SUP",
        "Affected section in the AIP",
        "Effective date and end time",
        "Reference to NOTAM being cancelled",
        "A publication date for AIP SUP",
        "A publication and effective date for AIRAC AIP SUP",
        "Reference to SUP(s) being replaced"
    );
  }

  private static List<Map<String, Object>> defaultAixmChecklist() {
    List<String> items = List.of(
        "AIXM schema version recorded",
        "Feature identification complete",
        "Geometry valid",
        "Temporal validity set",
        "Metadata present",
        "References resolved",
        "Validation report attached"
    );
    List<Map<String, Object>> rows = new ArrayList<>();
    for (String item : items) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("item", item);
      row.put("value", "");
      rows.add(row);
    }
    return rows;
  }

  private static String completenessColumn(String recordKind, boolean productQc) {
    String upper = recordKind.toUpperCase();
    if (upper.contains("AIC")) {
      return "AIC";
    }
    if (upper.contains("AMDT")) {
      return productQc ? "AIP AMDT" : "AIP";
    }
    return "AIP SUP/NOTAM";
  }
}
