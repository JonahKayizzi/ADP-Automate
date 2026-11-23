package ug.caa.adpautomate.airac;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/airac")
public class AiracController {

  public static final ZoneId UNIT_ZONE = ZoneId.of("Africa/Kampala");

  @GetMapping("/current")
  public Map<String, Object> current() {
    LocalDate today = LocalDate.now(UNIT_ZONE);
    AiracCalendar.AiracCycle cycle = AiracCalendar.cycleForDate(today);
    return Map.of(
        "today", today.toString(),
        "ident", cycle.ident(),
        "effective", cycle.effective().toString(),
        "distributeBy", cycle.distributeBy().toString(),
        "dayWithinCycle", AiracCalendar.dayWithinCycle(today, cycle.effective()),
        "cycleDays", AiracCalendar.CYCLE_DAYS
    );
  }

  @GetMapping("/cycles")
  public List<AiracCalendar.AiracCycleView> cycles(
      @RequestParam(defaultValue = "3") int future
  ) {
    LocalDate today = LocalDate.now(UNIT_ZONE);
    int count = Math.min(Math.max(future, 0), 14);
    return AiracCalendar.strip(today, count);
  }
}
