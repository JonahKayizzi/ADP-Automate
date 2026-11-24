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
}
