package ug.caa.adpautomate.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ug.caa.adpautomate.audit.AuditService.AuditEventResponse;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

  private final AuditService auditService;

  public AuditController(AuditService auditService) {
    this.auditService = auditService;
  }

  @GetMapping
  public List<AuditEventResponse> list(
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) String entityId,
      @RequestParam(defaultValue = "100") int limit
  ) {
    return auditService.list(entityType, entityId, limit);
  }
}
