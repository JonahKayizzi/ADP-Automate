package ug.caa.adpautomate.processing;

import org.junit.jupiter.api.Test;
import ug.caa.adpautomate.web.ApiException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigningRulesTest {

  @Test
  void of82RejectsMissingRequiredFields() {
    ApiException ex = assertThrows(
        ApiException.class,
        () -> SigningRules.requireFields("UCAA/DANS/AIM/OF/82", Map.of(), "qualityCheckedBy")
    );
    assertTrue(ex.getMessage().contains("Fill in"));
    assertTrue(ex.getMessage().contains("approver"));
  }

  @Test
  void of82AcceptsCompletePayload() {
    assertDoesNotThrow(() ->
        SigningRules.requireFields("UCAA/DANS/AIM/OF/82", completeOf82(), "qualityCheckedBy")
    );
  }

  @Test
  void confirmRequiresCheckFirst() {
    List<ProcessingSignature> slots = List.of(
        unsigned("qualityCheckedBy"),
        unsigned("qualityConfirmedBy")
    );
    ApiException ex = assertThrows(
        ApiException.class,
        () -> SigningRules.requireOrder("UCAA/DANS/AIM/OF/82", "qualityConfirmedBy", slots)
    );
    assertTrue(ex.getMessage().contains("check signature"));
  }

  @Test
  void confirmAllowedAfterCheck() {
    List<ProcessingSignature> slots = List.of(
        signed("qualityCheckedBy", "alice"),
        unsigned("qualityConfirmedBy")
    );
    assertDoesNotThrow(() ->
        SigningRules.requireOrder("UCAA/DANS/AIM/OF/82", "qualityConfirmedBy", slots)
    );
  }

  @Test
  void fourEyesRejectsSameConfirmer() {
    List<ProcessingSignature> slots = List.of(
        signed("qualityCheckedBy", "alice"),
        unsigned("qualityConfirmedBy")
    );
    ApiException ex = assertThrows(
        ApiException.class,
        () -> SigningRules.requireFourEyes(
            "UCAA/DANS/AIM/OF/82",
            "qualityConfirmedBy",
            slots,
            "alice"
        )
    );
    assertTrue(ex.getMessage().contains("different person"));
  }

  @Test
  void fourEyesAllowsDifferentConfirmer() {
    List<ProcessingSignature> slots = List.of(
        signed("qualityCheckedBy", "alice"),
        unsigned("qualityConfirmedBy")
    );
    assertDoesNotThrow(() ->
        SigningRules.requireFourEyes(
            "UCAA/DANS/AIM/OF/82",
            "qualityConfirmedBy",
            slots,
            "bob"
        )
    );
  }

  @Test
  void of83cRequiresUpdateBeforeFirstCheck() {
    List<ProcessingSignature> slots = List.of(
        unsigned("update"),
        unsigned("firstCheck"),
        unsigned("secondCheck")
    );
    ApiException ex = assertThrows(
        ApiException.class,
        () -> SigningRules.requireOrder("UCAA/DANS/AIM/OF/83c", "firstCheck", slots)
    );
    assertTrue(ex.getMessage().contains("Update"));
  }

  @Test
  void of83cSecondCheckMustDifferFromFirst() {
    List<ProcessingSignature> slots = List.of(
        signed("update", "alice"),
        signed("firstCheck", "bob"),
        unsigned("secondCheck")
    );
    ApiException ex = assertThrows(
        ApiException.class,
        () -> SigningRules.requireFourEyes(
            "UCAA/DANS/AIM/OF/83c",
            "secondCheck",
            slots,
            "bob"
        )
    );
    assertTrue(ex.getMessage().contains("2nd check"));
  }

  @Test
  void of83bRequiresMetadataFields() {
    ApiException ex = assertThrows(
        ApiException.class,
        () -> SigningRules.requireFields(
            "UCAA/DANS/AIM/OF/83b",
            Map.of("personResponsible", "x"),
            "preparedBy"
        )
    );
    assertTrue(ex.getMessage().contains("date performed"));
    assertTrue(ex.getMessage().contains("standards applied"));
  }

  private static Map<String, Object> completeOf82() {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("approver", "Manager");
    payload.put("approvalDate", "2026-10-19");
    payload.put("accuracy", "100%");
    payload.put("resolution", "100%");
    payload.put("integrity", "Routine");
    payload.put("traceability", "100%");
    payload.put("timeliness", "100%");
    payload.put("format", "100%");
    return payload;
  }

  private static ProcessingSignature unsigned(String key) {
    ProcessingSignature slot = new ProcessingSignature();
    slot.setId(UUID.randomUUID());
    slot.setFormId(UUID.randomUUID());
    slot.setSlotKey(key);
    slot.setSlotLabel(key);
    return slot;
  }

  private static ProcessingSignature signed(String key, String account) {
    ProcessingSignature slot = unsigned(key);
    slot.setSignedAt(Instant.parse("2026-10-20T10:00:00Z"));
    slot.setSignedByAccount(account);
    slot.setSignerName(account);
    return slot;
  }
}
