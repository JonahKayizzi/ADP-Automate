package ug.caa.adpautomate.airac;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * AIRAC cycle arithmetic for AIM Entebbe.
 * Anchor: AIRAC 2601 effective 22 Jan 2026. Cycles are 28 days.
 * Distribution deadline = effective date minus 42 days.
 */
public final class AiracCalendar {

  public static final LocalDate ANCHOR = LocalDate.of(2026, 1, 22);
  public static final int CYCLE_DAYS = 28;
  public static final int DISTRIBUTION_LEAD_DAYS = 42;

  private AiracCalendar() {}

  public static boolean isAiracDate(LocalDate date) {
    long days = ChronoUnit.DAYS.between(ANCHOR, date);
    return days >= 0 && days % CYCLE_DAYS == 0;
  }

  public static LocalDate cycleStartContaining(LocalDate date) {
    long days = ChronoUnit.DAYS.between(ANCHOR, date);
    if (days < 0) {
      long cyclesBack = Math.floorDiv(days, CYCLE_DAYS);
      return ANCHOR.plusDays(cyclesBack * (long) CYCLE_DAYS);
    }
    long cycles = days / CYCLE_DAYS;
    return ANCHOR.plusDays(cycles * (long) CYCLE_DAYS);
  }

  public static String identForEffectiveDate(LocalDate effective) {
    int dayOfYear = effective.getDayOfYear();
    int number = ((dayOfYear - 1) / CYCLE_DAYS) + 1;
    int year = effective.getYear() % 100;
    return String.format("%02d%02d", year, number);
  }

  public static LocalDate distributeBy(LocalDate effective) {
    return effective.minusDays(DISTRIBUTION_LEAD_DAYS);
  }

  public static int dayWithinCycle(LocalDate today, LocalDate cycleStart) {
    long day = ChronoUnit.DAYS.between(cycleStart, today) + 1;
    if (day < 1) {
      return 1;
    }
    if (day > CYCLE_DAYS) {
      return CYCLE_DAYS;
    }
    return (int) day;
  }

  public static AiracCycle cycleForDate(LocalDate date) {
    LocalDate effective = cycleStartContaining(date);
    return toCycle(effective);
  }

  public static AiracCycle toCycle(LocalDate effective) {
    return new AiracCycle(
        identForEffectiveDate(effective),
        effective,
        distributeBy(effective)
    );
  }

  /**
   * In-force cycle plus the next {@code futureCount} cycles, relative to {@code today}.
   */
  public static List<AiracCycleView> strip(LocalDate today, int futureCount) {
    LocalDate inForceStart = cycleStartContaining(today);
    List<AiracCycleView> result = new ArrayList<>();
    int dayOfCycle = dayWithinCycle(today, inForceStart);

    for (int i = 0; i <= futureCount; i++) {
      LocalDate effective = inForceStart.plusDays((long) i * CYCLE_DAYS);
      AiracCycle cycle = toCycle(effective);
      long daysToDeadline = ChronoUnit.DAYS.between(today, cycle.distributeBy());
      result.add(new AiracCycleView(
          cycle.ident(),
          cycle.effective(),
          cycle.distributeBy(),
          i == 0,
          daysToDeadline,
          dayOfCycle,
          0
      ));
    }
    return result;
  }

  public record AiracCycle(String ident, LocalDate effective, LocalDate distributeBy) {}

  public record AiracCycleView(
      String ident,
      LocalDate effective,
      LocalDate distributeBy,
      boolean inForce,
      long daysToDeadline,
      int dayWithinCycle,
      int publicationCount
  ) {}
}
