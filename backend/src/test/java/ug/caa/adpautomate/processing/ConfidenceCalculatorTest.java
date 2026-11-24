package ug.caa.adpautomate.processing;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfidenceCalculatorTest {

  @Test
  void allHundredPercentYields100() {
    Map<String, Object> payload = baseQuality("100%");
    payload.put("completeness", List.of(
        item("a", "100%"),
        item("b", "100%"),
        item("c", "N/A")
    ));
    ConfidenceCalculator.Result result = ConfidenceCalculator.calculate(payload);
    assertEquals(100, result.percent());
    assertEquals(7, result.scoredTotal());
    assertEquals(7, result.scored100());
    assertEquals(1, result.naOrBlank());
  }

  @Test
  void mixedScoresRoundCorrectly() {
    Map<String, Object> payload = baseQuality("100%");
    payload.put("accuracy", "100%");
    payload.put("resolution", "0%");
    payload.put("traceability", "100%");
    payload.put("timeliness", "0%");
    payload.put("format", "100%");
    payload.put("completeness", List.of(item("a", "100%"), item("b", "N/A"), item("c", "")));
    ConfidenceCalculator.Result result = ConfidenceCalculator.calculate(payload);
    // 4 of 6 scored at 100%
    assertEquals(6, result.scoredTotal());
    assertEquals(4, result.scored100());
    assertEquals(67, result.percent());
  }

  @Test
  void aixmIncludedAddsChecklist() {
    Map<String, Object> payload = baseQuality("100%");
    payload.put("completeness", List.of());
    payload.put("aixmIncluded", true);
    payload.put("aixmChecklist", List.of(
        Map.of("item", "Geometry", "value", "Yes"),
        Map.of("item", "Schema", "value", "No")
    ));
    ConfidenceCalculator.Result result = ConfidenceCalculator.calculate(payload);
    // 5 quality at 100% + Yes (100%) + No (0%) = 7 scored, 6 at 100%
    assertEquals(7, result.scoredTotal());
    assertEquals(6, result.scored100());
    assertEquals(86, result.percent());
  }

  @Test
  void aixmExcludedWhenNotIncluded() {
    Map<String, Object> payload = baseQuality("100%");
    payload.put("completeness", List.of());
    payload.put("aixmIncluded", false);
    payload.put("aixmChecklist", List.of(Map.of("item", "Geometry", "value", "No")));
    ConfidenceCalculator.Result result = ConfidenceCalculator.calculate(payload);
    assertEquals(5, result.scoredTotal());
    assertEquals(100, result.percent());
  }

  private static Map<String, Object> baseQuality(String score) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("accuracy", score);
    payload.put("resolution", score);
    payload.put("traceability", score);
    payload.put("timeliness", score);
    payload.put("format", score);
    return payload;
  }

  private static Map<String, Object> item(String name, String value) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("item", name);
    row.put("value", value);
    return row;
  }
}
