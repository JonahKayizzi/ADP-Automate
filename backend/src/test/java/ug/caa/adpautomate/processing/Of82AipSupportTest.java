package ug.caa.adpautomate.processing;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Of82AipSupportTest {

  @Test
  void detectsAipAmendmentKinds() {
    assertTrue(Of82AipSupport.isAipRecord("AIRAC AIP AMDT"));
    assertTrue(Of82AipSupport.isAipRecord("AIP AMDT"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void autoScoresFromAttachmentsIncludingSharedMetadataForm() {
    Map<String, Object> payload = new LinkedHashMap<>();
    List<Map<String, Object>> rows = new ArrayList<>();
    for (String item : List.of(
        Of82AipSupport.ITEM_SOURCE,
        Of82AipSupport.ITEM_COORDS,
        Of82AipSupport.ITEM_DATES,
        Of82AipSupport.ITEM_LAYOUT,
        Of82AipSupport.ITEM_PROMULGATION,
        Of82AipSupport.ITEM_SAFETY,
        Of82AipSupport.REMOVED_ITEM_METADATA,
        Of82AipSupport.REMOVED_ITEM_REGULATOR
    )) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("item", item);
      row.put("column", "AIP");
      row.put("value", "");
      rows.add(row);
    }
    payload.put("completeness", rows);

    Of82AipSupport.applyAutoCompleteness(
        payload,
        Set.of(
            "data_collection",
            "coordinate_verification",
            "promulgation",
            "metadata_form",
            "regulator_approval"
        )
    );

    List<Map<String, Object>> updated = (List<Map<String, Object>>) payload.get("completeness");
    assertEquals(6, updated.size());
    assertEquals("100%", valueOf(updated, Of82AipSupport.ITEM_SOURCE));
    assertEquals("100%", valueOf(updated, Of82AipSupport.ITEM_COORDS));
    assertEquals("100%", valueOf(updated, Of82AipSupport.ITEM_DATES));
    assertEquals("100%", valueOf(updated, Of82AipSupport.ITEM_LAYOUT));
    assertEquals("100%", valueOf(updated, Of82AipSupport.ITEM_PROMULGATION));
    assertEquals("", valueOf(updated, Of82AipSupport.ITEM_SAFETY));
  }

  private static String valueOf(List<Map<String, Object>> rows, String item) {
    return rows.stream()
        .filter(r -> item.equals(r.get("item")))
        .map(r -> String.valueOf(r.get("value")))
        .findFirst()
        .orElse("");
  }
}
