package ug.caa.adpautomate.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ug.caa.adpautomate.airac.AiracCalendar;
import ug.caa.adpautomate.airac.AiracController;
import ug.caa.adpautomate.notam.NotamService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  private final NotamService notamService;

  public DashboardController(NotamService notamService) {
    this.notamService = notamService;
  }

  @GetMapping("/summary")
  public Map<String, Object> summary() {
    LocalDate today = LocalDate.now(AiracController.UNIT_ZONE);
    List<AiracCalendar.AiracCycleView> cycles = AiracCalendar.strip(today, 3);
    return Map.of(
        "today", today.toString(),
        "notamsLogged", notamService.countActive(),
        "processingFilesNotSignedOff", 0,
        "publicationsNotYetEffective", 0,
        "openComplaints", 0,
        "overdueTasks", 0,
        "airac", cycles
    );
  }
}
