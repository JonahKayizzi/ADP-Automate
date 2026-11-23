package ug.caa.adpautomate.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ug.caa.adpautomate.airac.AiracCalendar;
import ug.caa.adpautomate.airac.AiracController;
import ug.caa.adpautomate.notam.NotamService;
import ug.caa.adpautomate.publication.PublicationService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  private final NotamService notamService;
  private final PublicationService publicationService;

  public DashboardController(NotamService notamService, PublicationService publicationService) {
    this.notamService = notamService;
    this.publicationService = publicationService;
  }

  @GetMapping("/summary")
  public Map<String, Object> summary() {
    LocalDate today = LocalDate.now(AiracController.UNIT_ZONE);
    List<AiracCalendar.AiracCycleView> base = AiracCalendar.strip(today, 3);
    List<LocalDate> dates = base.stream().map(AiracCalendar.AiracCycleView::effective).toList();
    Map<LocalDate, Long> counts = publicationService.airacCountsFor(dates);

    List<Map<String, Object>> airac = new ArrayList<>();
    for (AiracCalendar.AiracCycleView cycle : base) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("ident", cycle.ident());
      row.put("effective", cycle.effective().toString());
      row.put("distributeBy", cycle.distributeBy().toString());
      row.put("inForce", cycle.inForce());
      row.put("daysToDeadline", cycle.daysToDeadline());
      row.put("dayWithinCycle", cycle.dayWithinCycle());
      row.put("publicationCount", counts.getOrDefault(cycle.effective(), 0L));
      airac.add(row);
    }

    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("today", today.toString());
    summary.put("notamsLogged", notamService.countActive());
    summary.put("processingFilesNotSignedOff", 0);
    summary.put("publicationsNotYetEffective", publicationService.countNotYetEffective());
    summary.put("openComplaints", 0);
    summary.put("overdueTasks", 0);
    summary.put("airac", airac);
    return summary;
  }
}
