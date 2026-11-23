package ug.caa.adpautomate.airac;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiracCalendarTest {

  @Test
  void air001_calculates2611() {
    LocalDate effective = LocalDate.of(2026, 10, 29);
    assertEquals("2611", AiracCalendar.identForEffectiveDate(effective));
    assertTrue(AiracCalendar.isAiracDate(effective));
  }

  @Test
  void air002_calculates2612() {
    assertEquals("2612", AiracCalendar.identForEffectiveDate(LocalDate.of(2026, 11, 26)));
  }

  @Test
  void air003_calculates2701() {
    assertEquals("2701", AiracCalendar.identForEffectiveDate(LocalDate.of(2027, 1, 21)));
  }

  @Test
  void air004_distributionDeadlineIsEffectiveMinus42() {
    LocalDate effective = LocalDate.of(2026, 11, 26);
    assertEquals(LocalDate.of(2026, 10, 15), AiracCalendar.distributeBy(effective));
  }

  @Test
  void air005_rejectsNonAiracDate() {
    assertFalse(AiracCalendar.isAiracDate(LocalDate.of(2026, 10, 30)));
  }

  @Test
  void air006_deadlinePassedShowsNegativeDays() {
    LocalDate today = LocalDate.of(2026, 10, 29);
    List<AiracCalendar.AiracCycleView> strip = AiracCalendar.strip(today, 3);
    AiracCalendar.AiracCycleView cycle2612 = strip.stream()
        .filter(c -> c.ident().equals("2612"))
        .findFirst()
        .orElseThrow();
    assertTrue(cycle2612.daysToDeadline() < 0);
  }

  @Test
  void air007_yearTransitionIdent() {
    assertEquals("2613", AiracCalendar.identForEffectiveDate(LocalDate.of(2026, 12, 24)));
    assertEquals("2701", AiracCalendar.identForEffectiveDate(LocalDate.of(2027, 1, 21)));
  }

  @Test
  void referenceDatesMatchSpec() {
    assertEquals("2610", AiracCalendar.identForEffectiveDate(LocalDate.of(2026, 10, 1)));
    assertEquals(LocalDate.of(2026, 10, 29), AiracCalendar.cycleStartContaining(LocalDate.of(2026, 10, 29)));
    assertEquals(1, AiracCalendar.dayWithinCycle(LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 29)));
  }
}
