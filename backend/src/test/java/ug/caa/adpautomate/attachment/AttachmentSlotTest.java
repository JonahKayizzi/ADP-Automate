package ug.caa.adpautomate.attachment;

import org.junit.jupiter.api.Test;
import ug.caa.adpautomate.web.ApiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttachmentSlotTest {

  @Test
  void libraryIgnoresSlot() {
    assertNull(AttachmentService.normalizeSlot(AttachmentOwnerType.LIBRARY, "supporting"));
  }

  @Test
  void notamRequiresSupportingOrProduct() {
    assertEquals("supporting", AttachmentService.normalizeSlot(AttachmentOwnerType.NOTAM, "supporting"));
    assertEquals("product", AttachmentService.normalizeSlot(AttachmentOwnerType.NOTAM, "PRODUCT"));
    assertThrows(ApiException.class, () -> AttachmentService.normalizeSlot(AttachmentOwnerType.NOTAM, null));
    assertThrows(ApiException.class, () -> AttachmentService.normalizeSlot(AttachmentOwnerType.NOTAM, "publication"));
  }

  @Test
  void publicationDefaultsToPublicationSlot() {
    assertEquals("publication", AttachmentService.normalizeSlot(AttachmentOwnerType.PUBLICATION, null));
    assertEquals("publication", AttachmentService.normalizeSlot(AttachmentOwnerType.PUBLICATION, "file"));
    assertEquals("promulgation", AttachmentService.normalizeSlot(AttachmentOwnerType.PUBLICATION, "promulgation"));
    assertEquals("data_collection", AttachmentService.normalizeSlot(AttachmentOwnerType.PUBLICATION, "data_collection"));
    assertEquals(
        "coordinate_verification",
        AttachmentService.normalizeSlot(AttachmentOwnerType.PUBLICATION, "coordinate_verification")
    );
    assertEquals(
        "metadata_form",
        AttachmentService.normalizeSlot(AttachmentOwnerType.PUBLICATION, "metadata_form")
    );
    assertEquals(
        "safety_assessment",
        AttachmentService.normalizeSlot(AttachmentOwnerType.PUBLICATION, "safety_assessment")
    );
    assertEquals(
        "regulator_approval",
        AttachmentService.normalizeSlot(AttachmentOwnerType.PUBLICATION, "regulator_approval")
    );
    assertThrows(ApiException.class, () -> AttachmentService.normalizeSlot(AttachmentOwnerType.PUBLICATION, "product"));
  }
}
