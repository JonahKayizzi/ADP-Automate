package ug.caa.adpautomate.processing;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingFormCatalogTest {

  @Test
  void of83bHasFourteenPrincipalFields() {
    Map<String, Object> payload = ProcessingFormCatalog.emptyOf83b();
    assertEquals(
        List.of(
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
        ),
        List.copyOf(payload.keySet())
    );
    assertFalse(payload.containsKey("rows"));
  }

  @Test
  void of83aIncludesAdqAndSharedQualityFields() {
    Map<String, Object> payload = ProcessingFormCatalog.emptyOf83a();
    assertTrue(payload.containsKey("adqCompliance"));
    assertTrue(payload.containsKey("reviewDate"));
    assertTrue(payload.containsKey("operation"));
    assertTrue(payload.containsKey("personResponsible"));
    assertTrue(payload.containsKey("standardsApplied"));
    assertTrue(payload.containsKey("effectiveDate"));
    assertTrue(payload.containsKey("effectiveTime"));
    assertTrue(payload.containsKey("toolVersion"));
    assertFalse(payload.containsKey("notes"));
    assertFalse(payload.containsKey("effectiveDateTime"));
  }

  @Test
  void amdUses83aAndNotamUses83b() {
    List<ProcessingFormCatalog.FormDef> notam = ProcessingFormCatalog.forRecordKind("NOTAM");
    List<ProcessingFormCatalog.FormDef> amd = ProcessingFormCatalog.forRecordKind("AIRAC AIP AMDT");
    assertEquals("UCAA/DANS/AIM/OF/83b", notam.get(1).code());
    assertEquals("UCAA/DANS/AIM/OF/83a", amd.get(1).code());
  }

  @Test
  @SuppressWarnings("unchecked")
  void aipOf82CompletenessDropsCrossReferencesAndAddsAttachmentDrivenItems() {
    Map<String, Object> payload = ProcessingFormCatalog.forRecordKind("AIRAC AIP AMDT").get(0).emptyPayload();
    List<Map<String, Object>> completeness = (List<Map<String, Object>>) payload.get("completeness");
    List<String> items = completeness.stream().map(r -> String.valueOf(r.get("item"))).toList();
    assertEquals(
        List.of(
            "All source data received",
            "Coordinates verified",
            "Effective dates confirmed",
            "Layout / format reviewed",
            "Signed promulgation form",
            "Safety assessment if required"
        ),
        items
    );
    assertTrue(payload.containsKey("accuracyComment"));
    assertEquals(true, payload.get("confidenceManual"));
  }

  @Test
  void normalizeMergesKnownKeysAndDropsLegacyRows() {
    Map<String, Object> stored = new LinkedHashMap<>();
    stored.put("personResponsible", "Airspace planning officer");
    stored.put("rows", List.of(Map.of("label", "Originator", "value", "x")));
    stored.put("unknownExtra", "keep-out");

    Map<String, Object> normalized =
        ProcessingFormCatalog.normalizePayload("UCAA/DANS/AIM/OF/83b", stored);

    assertEquals("Airspace planning officer", normalized.get("personResponsible"));
    assertEquals("", normalized.get("role"));
    assertFalse(normalized.containsKey("rows"));
    assertFalse(normalized.containsKey("unknownExtra"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void of83cHasNineSystemsWithNotamPromulgationSteps() {
    Map<String, Object> payload = ProcessingFormCatalog.emptyOf83c();
    List<Map<String, Object>> systems = (List<Map<String, Object>>) payload.get("systems");
    assertEquals(9, systems.size());
    Map<String, Object> notam = systems.stream()
        .filter(s -> "NOTAM".equals(s.get("name")))
        .findFirst()
        .orElseThrow();
    List<Map<String, Object>> steps = (List<Map<String, Object>>) notam.get("steps");
    assertEquals(5, steps.size());
    assertEquals("Prepare NOTAM promulgation form", steps.get(0).get("label"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void normalizeOf83cPreservesCheckedSteps() {
    Map<String, Object> stored = ProcessingFormCatalog.emptyOf83c();
    List<Map<String, Object>> systems = (List<Map<String, Object>>) stored.get("systems");
    Map<String, Object> notam = systems.stream()
        .filter(s -> "NOTAM".equals(s.get("name")))
        .findFirst()
        .orElseThrow();
    List<Map<String, Object>> steps = (List<Map<String, Object>>) notam.get("steps");
    steps.get(0).put("done", "Yes");

    Map<String, Object> normalized =
        ProcessingFormCatalog.normalizePayload("UCAA/DANS/AIM/OF/83c", stored);
    List<Map<String, Object>> mergedSystems = (List<Map<String, Object>>) normalized.get("systems");
    Map<String, Object> mergedNotam = mergedSystems.stream()
        .filter(s -> "NOTAM".equals(s.get("name")))
        .findFirst()
        .orElseThrow();
    List<Map<String, Object>> mergedSteps = (List<Map<String, Object>>) mergedNotam.get("steps");
    assertEquals("Yes", mergedSteps.get(0).get("done"));
    assertEquals("", mergedSteps.get(1).get("done"));
  }
}
