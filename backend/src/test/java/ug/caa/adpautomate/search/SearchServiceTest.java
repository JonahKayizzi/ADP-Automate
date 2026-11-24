package ug.caa.adpautomate.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchServiceTest {

  @Test
  void tokenizeSplitsOnWhitespace() {
    assertEquals(List.of("a0234", "vor"), SearchService.tokenize("  A0234   VOR "));
  }

  @Test
  void singleTermMatchesAnyField() {
    assertTrue(SearchService.matches(List.of("vor"), "A0234/26", "EBB VOR unserviceable"));
    assertFalse(SearchService.matches(List.of("ils"), "A0234/26", "EBB VOR unserviceable"));
  }

  @Test
  void multiTermRequiresEveryToken() {
    assertTrue(SearchService.matches(
        List.of("a0234", "vor"),
        "A0234/26",
        "EBB VOR unserviceable",
        "CNS / Navaids"
    ));
    assertFalse(SearchService.matches(
        List.of("a0234", "ils"),
        "A0234/26",
        "EBB VOR unserviceable",
        "CNS / Navaids"
    ));
  }

  @Test
  void termsCanSpanDifferentFields() {
    assertTrue(SearchService.matches(
        List.of("airac", "01/2027"),
        "01/2027",
        "Revised ATS routes",
        "AIRAC AIP AMDT"
    ));
  }

  @Test
  void limitIsTwelve() {
    assertEquals(12, SearchService.LIMIT);
  }
}
