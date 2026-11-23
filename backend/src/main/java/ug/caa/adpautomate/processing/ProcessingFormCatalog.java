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
    payload.put("completeness", defaultCompleteness(recordKind));
    payload.put("aixmChecklist", defaultAixmChecklist());
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

  private static FormDef of83b() {
    Map<String, Object> payload = new LinkedHashMap<>();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (String label : List.of(
        "Originator",
        "NOTAM / AIC / SUP number",
        "Subject",
        "Purpose",
        "Scope",
        "Effective date / time",
        "Valid until",
        "Lower / upper limit",
        "Coordinates / location",
        "Q-line / schedule",
        "Text of NOTAM / AIC / SUP",
        "References",
        "Remarks",
        "Distribution"
    )) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("label", label);
      row.put("value", "");
      rows.add(row);
    }
    payload.put("rows", rows);
    return new FormDef(
        2,
        "UCAA/DANS/AIM/OF/83b",
        "AIC/NOTAM/SUP meta data form",
        "Controlled document — MANSOPS Part III (Rev 04, 01/08/2023)",
        List.of(new SlotDef("preparedBy", "Prepared by")),
        payload
    );
  }

  private static FormDef of83a() {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("adqCompliance", "");
    payload.put("reviewDate", "");
    payload.put("operation", "");
    payload.put("effectiveDate", "");
    payload.put("effectiveTime", "");
    payload.put("notes", "");
    return new FormDef(
        2,
        "UCAA/DANS/AIM/OF/83a",
        "AIP meta data form",
        "Controlled document — MANSOPS Part III (Rev 04, 01/08/2023)",
        List.of(new SlotDef("preparedBy", "Prepared by")),
        payload
    );
  }

  private static FormDef of83c() {
    Map<String, Object> payload = new LinkedHashMap<>();
    List<Map<String, Object>> systems = new ArrayList<>();
    for (String name : List.of(
        "AIM Static Database",
        "ACM",
        "Framemaker Text",
        "Graphics",
        "AIP-AIM Static Database Data Linkage",
        "Esri Database",
        "AIS Static Database",
        "NOTAM",
        "Data sets"
    )) {
      Map<String, Object> system = new LinkedHashMap<>();
      system.put("name", name);
      system.put("steps", List.of(
          Map.of("label", "Data prepared", "done", ""),
          Map.of("label", "Data entered / updated", "done", ""),
          Map.of("label", "Verified", "done", "")
      ));
      systems.add(system);
    }
    payload.put("systems", systems);
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
    payload.put("part1", part1);
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
        payload
    );
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
    payload.put("applicableRegulations", "");
    payload.put("completeness", defaultCompleteness(recordKind));
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

  private static List<Map<String, Object>> defaultCompleteness(String recordKind) {
    String column = completenessColumn(recordKind);
    List<String> items = List.of(
        "All source data received",
        "Coordinates verified",
        "Effective dates confirmed",
        "Cross-references checked",
        "Layout / format reviewed"
    );
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

  private static String completenessColumn(String recordKind) {
    String upper = recordKind.toUpperCase();
    if (upper.contains("AIC")) {
      return "AIC";
    }
    if (upper.contains("AMDT")) {
      return "AIP AMDT";
    }
    return "AIP SUP/NOTAM";
  }
}
