package ug.caa.adpautomate.processing;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import ug.caa.adpautomate.processing.dto.ProcessingDtos.SignatureResponse;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Official MANSOPS Appendix 4 / OF/82 landscape layout.
 */
final class Of82PdfExporter {

  private static final ZoneId KAMPALA = ZoneId.of("Africa/Kampala");
  private static final DateTimeFormatter DT =
      DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(KAMPALA);

  private static final Color YELLOW = new Color(255, 242, 0);
  private static final Color FOOTER_RULE = new Color(99, 36, 35); // burgundy
  private static final Color GRID = Color.BLACK;
  private static final float MARGIN = 28f;
  private static final float BODY = 10f;
  private static final float HF = 11f;

  private Of82PdfExporter() {
  }

  static byte[] export(
      String formCode,
      String formTitle,
      String recordKind,
      Map<String, Object> payload,
      List<SignatureResponse> signatures
  ) throws IOException {
    try (PDDocument doc = new PDDocument()) {
      Fonts fonts = loadFonts(doc);
      PDImageXObject logo = loadLogo(doc);

      drawForm(doc, fonts, logo, payload, recordKind, signatures,
          formCode == null || formCode.isBlank() ? "UCAA/DANS/AIM/OF/82" : formCode);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      doc.save(out);
      return out.toByteArray();
    }
  }

  private static void drawForm(
      PDDocument doc,
      Fonts fonts,
      PDImageXObject logo,
      Map<String, Object> payload,
      String recordKind,
      List<SignatureResponse> signatures,
      String formCode
  ) throws IOException {
    PDPage page = newLandscapePage();
    doc.addPage(page);
    float pageW = page.getMediaBox().getWidth();
    float pageH = page.getMediaBox().getHeight();
    float contentW = pageW - 2 * MARGIN;
    float footerReserve = 42f;

    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
      float y = pageH - MARGIN;
      y = drawHeader(cs, fonts, logo, y, pageW);
      y -= 6;

      // Analysis of raw data
      y = yellowBanner(cs, fonts.bold, y, MARGIN, contentW, "Analysis of raw data");
      float findingsH = 36;
      float findingsLabelW = 70;
      strokeRect(cs, MARGIN, y - findingsH, findingsLabelW, findingsH);
      strokeRect(cs, MARGIN + findingsLabelW, y - findingsH, contentW - findingsLabelW, findingsH);
      textAt(cs, fonts.regular, BODY, MARGIN + 4, y - 14, "Findings");
      drawWrapped(cs, fonts.regular, BODY, MARGIN + findingsLabelW + 4, y - 14,
          contentW - findingsLabelW - 8, text(payload.get("findings")), findingsH - 6);
      y -= findingsH;

      // Approval
      y = yellowBanner(cs, fonts.bold, y, MARGIN, contentW, "Approval");
      float apprLabelW = contentW * 0.55f;
      y = headerPair(cs, fonts.bold, y, MARGIN, apprLabelW, contentW - apprLabelW, "Item", "Value");
      y = valuePair(cs, fonts.regular, y, MARGIN, apprLabelW, contentW - apprLabelW, 26,
          "Approver (Person who approved the data for release to the AIS)",
          text(payload.get("approver")));
      y = valuePair(cs, fonts.regular, y, MARGIN, apprLabelW, contentW - apprLabelW, 18,
          "Date of approval", text(payload.get("approvalDate")));

      // Quality Check
      y = yellowBanner(cs, fonts.bold, y, MARGIN, contentW,
          "Quality Check (* Insert either 100% for compliance or 0% for non-compliance)");
      float checkColW = contentW * 0.42f;
      float itemColW = contentW - checkColW;
      y = headerPair(cs, fonts.bold, y, MARGIN, itemColW, checkColW, "Item", "Check");

      y = qcSimple(cs, fonts, y, MARGIN, itemColW, checkColW,
          "Accuracy", isHundred(payload.get("accuracy")), text(payload.get("accuracyComment")));
      y = qcSimple(cs, fonts, y, MARGIN, itemColW, checkColW,
          "Resolution", isHundred(payload.get("resolution")), text(payload.get("resolutionComment")));
      y = qcIntegrity(cs, fonts, y, MARGIN, itemColW, checkColW, text(payload.get("integrity")),
          text(payload.get("integrityComment")));
      y = qcSimple(cs, fonts, y, MARGIN, itemColW, checkColW,
          "Traceability", isHundred(payload.get("traceability")), text(payload.get("traceabilityComment")));
      y = qcTimeliness(cs, fonts, y, MARGIN, itemColW, checkColW,
          isHundred(payload.get("timeliness")),
          text(payload.get("dateReceived")),
          text(payload.get("publicationDate")),
          text(payload.get("timelinessComment")));
      y = qcFormat(cs, fonts, y, MARGIN, itemColW, checkColW,
          text(payload.get("fileType")),
          isHundred(payload.get("format")),
          text(payload.get("formatComment")));

      // Completeness title row (merged) then 4-column grid + confidence as table continuation
      Map<String, String> scores = completenessScores(payload);
      String active = activeColumn(recordKind, payload);
      boolean aixmOn = Boolean.TRUE.equals(payload.get("aixmIncluded"));
      if (aixmOn && payload.get("aixmChecklist") instanceof List<?> aixmRows) {
        for (Object row : aixmRows) {
          if (row instanceof Map<?, ?> map) {
            scores.put(text(map.get("item")), text(map.get("value")));
          }
        }
      }

      float remaining = y - footerReserve - 8;
      float needed = estimateCompletenessHeight() + estimateSignatureHeight() + 24;
      if (remaining >= needed) {
        drawCompletenessAndSignatures(cs, fonts, y, MARGIN, contentW, scores, active, aixmOn,
            payload, signatures);
        drawFooter(cs, fonts, pageW, formCode);
        return;
      }
      drawFooter(cs, fonts, pageW, formCode);
    }

    // Page 2 when page 1 cannot fit completeness + signatures
    PDPage page2 = newLandscapePage();
    doc.addPage(page2);
    float pageW2 = page2.getMediaBox().getWidth();
    float pageH2 = page2.getMediaBox().getHeight();
    float contentW2 = pageW2 - 2 * MARGIN;
    try (PDPageContentStream cs = new PDPageContentStream(doc, page2)) {
      float y = pageH2 - MARGIN;
      y = drawHeader(cs, fonts, logo, y, pageW2);
      y -= 4;
      Map<String, String> scores = completenessScores(payload);
      String active = activeColumn(recordKind, payload);
      boolean aixmOn = Boolean.TRUE.equals(payload.get("aixmIncluded"));
      if (aixmOn && payload.get("aixmChecklist") instanceof List<?> aixmRows) {
        for (Object row : aixmRows) {
          if (row instanceof Map<?, ?> map) {
            scores.put(text(map.get("item")), text(map.get("value")));
          }
        }
      }
      drawCompletenessAndSignatures(cs, fonts, y, MARGIN, contentW2, scores, active, aixmOn,
          payload, signatures);
      drawFooter(cs, fonts, pageW2, formCode);
    }
  }

  private static float drawCompletenessAndSignatures(
      PDPageContentStream cs,
      Fonts fonts,
      float y,
      float x,
      float contentW,
      Map<String, String> scores,
      String active,
      boolean aixmOn,
      Map<String, Object> payload,
      List<SignatureResponse> signatures
  ) throws IOException {
    // Completeness header spanning full width
    float titleH = 16;
    strokeRect(cs, x, y - titleH, contentW, titleH);
    textAt(cs, fonts.bold, BODY, x + 4, y - 11, "*Completeness (refer to the parameters below)");
    // tick box on the right of title? sample has small box in item area - put tick if overall ok
    float tickBox = 12;
    strokeRect(cs, x + contentW - 18, y - 14, tickBox, tickBox);
    if ("100%".equals(completenessOverall(scores))) {
      textAt(cs, fonts.bold, 9, x + contentW - 16, y - 12, "X");
    }
    y -= titleH;

    String[] groups = {"AIP SUP/NOTAM", "AIC", "AIP", "AIXM File"};
    List<List<String>> items = officialCompletenessItems();
    float colW = contentW / 4f;
    float checkW = 26f;
    float itemW = colW - checkW;
    float headH = 14;
    float subH = 12;
    float rowH = 28; // tall enough for full official wording with wrap

    for (int c = 0; c < 4; c++) {
      float cx = x + c * colW;
      strokeRect(cs, cx, y - headH, colW, headH);
      centered(cs, fonts.bold, 9, cx, y - headH, colW, headH, groups[c]);
    }
    y -= headH;

    for (int c = 0; c < 4; c++) {
      float cx = x + c * colW;
      strokeRect(cs, cx, y - subH, itemW, subH);
      strokeRect(cs, cx + itemW, y - subH, checkW, subH);
      textAt(cs, fonts.bold, 8, cx + 2, y - 9, "Item");
      centered(cs, fonts.bold, 8, cx + itemW, y - subH, checkW, subH, "Check");
    }
    y -= subH;

    int maxRows = items.stream().mapToInt(List::size).max().orElse(0);
    for (int r = 0; r < maxRows; r++) {
      for (int c = 0; c < 4; c++) {
        float cx = x + c * colW;
        strokeRect(cs, cx, y - rowH, itemW, rowH);
        strokeRect(cs, cx + itemW, y - rowH, checkW, rowH);
        List<String> col = items.get(c);
        if (r < col.size()) {
          String item = col.get(r);
          if (!item.isBlank()) {
            drawWrapped(cs, fonts.regular, 7.5f, cx + 2, y - 9, itemW - 4, item, rowH - 4);
            boolean colActive = groups[c].equals(active)
                || ("AIXM File".equals(groups[c]) && aixmOn);
            if (colActive && isChecked(resolveScore(scores, item, groups[c], payload))) {
              centered(cs, fonts.bold, 9, cx + itemW, y - rowH, checkW, rowH, "X");
            }
          }
        }
      }
      y -= rowH;
    }

    // CONFIDENCE LEVEL — next row of the same table (not separated)
    float confH = 16;
    float confLabelW = colW;
    strokeRect(cs, x, y - confH, confLabelW, confH);
    strokeRect(cs, x + confLabelW, y - confH, contentW - confLabelW, confH);
    textAt(cs, fonts.bold, BODY, x + 4, y - 11, "CONFIDENCE LEVEL");
    textAt(cs, fonts.regular, BODY, x + confLabelW + 6, y - 11, text(payload.get("confidenceLevel")));
    y -= confH;

    // Signatures fixed at bottom of this (last) page, above the footer
    float sigTop = 30f + 54f; // just above footer band
    textAt(cs, fonts.italic, 8, x, sigTop + 40,
        "*100% for compliance, 0% for non-compliance");
    float sigW = (contentW - 24) / 2f;
    drawSignatureLines(cs, fonts, x, sigTop + 24, sigW,
        "Raw data quality checked by", findSig(signatures, "qualityCheckedBy"));
    drawSignatureLines(cs, fonts, x + sigW + 24, sigTop + 24, sigW,
        "Raw data quality check confirmed by", findSig(signatures, "qualityConfirmedBy"));
    return Math.min(y, sigTop) - 8;
  }

  private static float estimateCompletenessHeight() {
    return 16 + 14 + 12 + (8 * 28) + 16 + 70;
  }

  private static float estimateSignatureHeight() {
    return 70;
  }

  /** Exact official OF/82 completeness cell text (row-aligned across columns). */
  private static List<List<String>> officialCompletenessItems() {
    return List.of(
        List.of(
            "A declaration that the requirements of the data have been met including applicable standards",
            "Effective date and time of the information",
            "End date and time of the information",
            "Daily schedule if required",
            "Approval from the regulator if required",
            "Meta data for AIP SUP/NOTAM",
            "Signed promulgation form",
            "Safety assessment if required"
        ),
        List.of(
            "A declaration that the requirements of the data have been met including applicable standards",
            "Effective date of the information",
            "End date of the information",
            "Safety assessment if required",
            "Approval from the regulator if required",
            "Meta data for AIC",
            "",
            ""
        ),
        List.of(
            "Data collection form",
            "Signed promulgation form",
            "A declaration that the requirements of the data have been met including applicable standards",
            "Effective date of the information",
            "End date of the information",
            "Safety assessment if required",
            "Approval from the regulator if required",
            "Meta data for AIP"
        ),
        List.of(
            "Meta data",
            "A declaration that the requirements of the data have been met including applicable standards",
            "Effective date and time of the information",
            "End date and time of the information",
            "Safety assessment if required",
            "Approval from the regulator if required",
            "Compatibility with AIM database",
            ""
        )
    );
  }

  private static String resolveScore(
      Map<String, String> scores,
      String officialItem,
      String group,
      Map<String, Object> payload
  ) {
    // Direct match
    if (scores.containsKey(officialItem) && !text(scores.get(officialItem)).isBlank()) {
      return scores.get(officialItem);
    }
    // Map app completeness labels → official wording
    Map<String, String> aliases = Map.ofEntries(
        Map.entry("Data collection form", "All source data received"),
        Map.entry("Signed promulgation form", "Signed promulgation form"),
        Map.entry("Effective date of the information", "Effective dates confirmed"),
        Map.entry("Effective date and time of the information", "Effective dates confirmed"),
        Map.entry("End date of the information", "Effective dates confirmed"),
        Map.entry("End date and time of the information", "Effective dates confirmed"),
        Map.entry("Meta data for AIP", "Meta data for AIP"),
        Map.entry("Meta data", "Meta data for AIP"),
        Map.entry("Safety assessment if required", "Safety assessment if required"),
        Map.entry("Approval from the regulator if required", "Approval from the regulator if required"),
        Map.entry("Layout / format reviewed", "Layout / format reviewed")
    );
    // metadata_form attachment drives meta data + dates in our app
    String appKey = aliases.get(officialItem);
    if (appKey != null && scores.containsKey(appKey)) {
      return scores.get(appKey);
    }
    // Meta data form attachment marks metadata rows when AIP dates/layout are 100%
    if (("Meta data for AIP".equals(officialItem) || "Meta data".equals(officialItem)
        || "Meta data for AIC".equals(officialItem) || "Meta data for AIP SUP/NOTAM".equals(officialItem))
        && isChecked(scores.getOrDefault("Effective dates confirmed", ""))
        && isChecked(scores.getOrDefault("Layout / format reviewed", ""))) {
      return "100%";
    }
    if ("Data collection form".equals(officialItem)) {
      return scores.getOrDefault("All source data received", "");
    }
    if (officialItem.startsWith("A declaration that the requirements")) {
      // treat as met when key completeness items are 100% for active product
      if ("AIP".equals(group)
          && isChecked(scores.getOrDefault("All source data received", ""))
          && isChecked(scores.getOrDefault("Signed promulgation form", ""))) {
        return "100%";
      }
    }
    if ("Compatibility with AIM database".equals(officialItem)
        && Boolean.TRUE.equals(payload.get("aixmIncluded"))) {
      return scores.getOrDefault("Validation report attached", "");
    }
    return scores.getOrDefault(officialItem, "");
  }

  private static float qcSimple(
      PDPageContentStream cs,
      Fonts fonts,
      float y,
      float x,
      float itemW,
      float checkW,
      String label,
      boolean tick,
      String comment
  ) throws IOException {
    float h = Math.max(18, comment.isBlank() ? 18 : 26);
    strokeRect(cs, x, y - h, itemW, h);
    strokeRect(cs, x + itemW, y - h, checkW, h);
    textAt(cs, fonts.regular, BODY, x + 4, y - 12, "*" + label);
    float box = 11;
    float bx = x + 4 + stringWidth(fonts.regular, BODY, "*" + label) + 6;
    strokeRect(cs, bx, y - 14, box, box);
    if (tick) {
      textAt(cs, fonts.bold, 9, bx + 2, y - 12, "X");
    }
    if (!comment.isBlank()) {
      drawWrapped(cs, fonts.regular, BODY, x + itemW + 4, y - 12, checkW - 8, comment, h - 4);
    }
    return y - h;
  }

  private static float qcIntegrity(
      PDPageContentStream cs,
      Fonts fonts,
      float y,
      float x,
      float itemW,
      float checkW,
      String integrity,
      String comment
  ) throws IOException {
    float rowH = 14;
    float h = rowH * 3;
    strokeRect(cs, x, y - h, itemW, h);
    textAt(cs, fonts.regular, BODY, x + 4, y - 16, "Integrity");
    if (!comment.isBlank()) {
      textAt(cs, fonts.regular, 8, x + 4, y - h + 4, truncate(comment, 55));
    }
    String[] levels = {"Routine", "Essential", "Critical"};
    for (int i = 0; i < 3; i++) {
      float ry = y - i * rowH;
      strokeRect(cs, x + itemW, ry - rowH, checkW, rowH);
      boolean selected = levels[i].equalsIgnoreCase(integrity);
      float box = 9;
      strokeRect(cs, x + itemW + 4, ry - 11, box, box);
      if (selected) {
        textAt(cs, fonts.bold, 8, x + itemW + 5.5f, ry - 9, "X");
      }
      textAt(cs, fonts.regular, BODY, x + itemW + 18, ry - 10, levels[i]);
    }
    return y - h;
  }

  private static float qcTimeliness(
      PDPageContentStream cs,
      Fonts fonts,
      float y,
      float x,
      float itemW,
      float checkW,
      boolean tick,
      String dateReceived,
      String publicationDate,
      String comment
  ) throws IOException {
    float h = 28;
    strokeRect(cs, x, y - h, itemW, h);
    textAt(cs, fonts.regular, BODY, x + 4, y - 12, "*Timeliness");
    float box = 11;
    float bx = x + 4 + stringWidth(fonts.regular, BODY, "*Timeliness") + 6;
    strokeRect(cs, bx, y - 14, box, box);
    if (tick) {
      textAt(cs, fonts.bold, 9, bx + 2, y - 12, "X");
    }
    if (!comment.isBlank()) {
      textAt(cs, fonts.regular, 8, x + 4, y - 24, truncate(comment, 60));
    }
    float half = checkW / 2f;
    strokeRect(cs, x + itemW, y - h, half, h);
    strokeRect(cs, x + itemW + half, y - h, half, h);
    textAt(cs, fonts.regular, 8, x + itemW + 3, y - 10, "Date Received:");
    textAt(cs, fonts.regular, BODY, x + itemW + 3, y - 22, dateReceived);
    textAt(cs, fonts.regular, 8, x + itemW + half + 3, y - 10, "Publication date:");
    textAt(cs, fonts.regular, BODY, x + itemW + half + 3, y - 22, publicationDate);
    return y - h;
  }

  private static float qcFormat(
      PDPageContentStream cs,
      Fonts fonts,
      float y,
      float x,
      float itemW,
      float checkW,
      String fileType,
      boolean tick,
      String comment
  ) throws IOException {
    float h = Math.max(22, comment.isBlank() ? 22 : 28);
    strokeRect(cs, x, y - h, itemW, h);
    strokeRect(cs, x + itemW, y - h, checkW, h);
    textAt(cs, fonts.regular, BODY, x + 4, y - 12, "*Format (File Type)");
    float boxX = x + 4 + stringWidth(fonts.regular, BODY, "*Format (File Type)") + 8;
    float boxW = Math.min(90, itemW - (boxX - x) - 8);
    strokeRect(cs, boxX, y - 16, boxW, 12);
    textAt(cs, fonts.regular, 8, boxX + 3, y - 13, truncate(fileType, 18));
    if (tick) {
      // small compliance tick after the file-type box
      strokeRect(cs, boxX + boxW + 4, y - 14, 10, 10);
      textAt(cs, fonts.bold, 8, boxX + boxW + 5.5f, y - 12, "X");
    }
    if (!comment.isBlank()) {
      drawWrapped(cs, fonts.regular, BODY, x + itemW + 4, y - 12, checkW - 8, comment, h - 4);
    }
    return y - h;
  }

  private static float drawSignatureLines(
      PDPageContentStream cs,
      Fonts fonts,
      float x,
      float y,
      float w,
      String title,
      SignatureResponse sig
  ) throws IOException {
    String name = sig != null && Boolean.TRUE.equals(sig.signed()) ? nullSafe(sig.signerName()) : "";
    String designation = sig != null && Boolean.TRUE.equals(sig.signed()) ? nullSafe(sig.designation()) : "";
    String signed = "";
    if (sig != null && Boolean.TRUE.equals(sig.signed()) && sig.signedAt() != null) {
      signed = DT.format(sig.signedAt());
    }

    // Label + dotted leader line; filled value sits on the line
    y = signatureField(cs, fonts, x, y, w, title + ":", name);
    y = signatureField(cs, fonts, x, y, w, "Designation:", designation);
    y = signatureField(cs, fonts, x, y, w, "Signature:", signed);
    return y;
  }

  private static float signatureField(
      PDPageContentStream cs,
      Fonts fonts,
      float x,
      float y,
      float w,
      String label,
      String value
  ) throws IOException {
    textAt(cs, fonts.regular, BODY, x, y, label);
    float labelW = stringWidth(fonts.regular, BODY, label) + 4;
    float lineStart = x + labelW;
    float lineEnd = x + w;
    dottedLine(cs, lineStart, y - 1, lineEnd, y - 1);
    if (value != null && !value.isBlank()) {
      textAt(cs, fonts.regular, BODY, lineStart + 2, y, value);
    }
    return y - 14;
  }

  private static void dottedLine(PDPageContentStream cs, float x1, float y1, float x2, float y2)
      throws IOException {
    cs.setStrokingColor(GRID);
    cs.setLineWidth(0.9f);
    cs.setLineDashPattern(new float[] {1.2f, 1.8f}, 0);
    cs.moveTo(x1, y1);
    cs.lineTo(x2, y2);
    cs.stroke();
    cs.setLineDashPattern(new float[] {}, 0);
    cs.setLineWidth(0.7f);
  }

  private static float drawHeader(
      PDPageContentStream cs,
      Fonts fonts,
      PDImageXObject logo,
      float y,
      float pageW
  ) throws IOException {
    float logoH = 36;
    float logoW = 72;
    if (logo != null) {
      cs.drawImage(logo, MARGIN, y - logoH, logoW, logoH);
    }
    centered(cs, fonts.bold, HF, MARGIN, y - 14, pageW - 2 * MARGIN, 14, "MANSOPS PART III - AIM");
    textAt(cs, fonts.bold, HF, pageW - MARGIN - 70, y - 12, "Appendix 4");
    centered(cs, fonts.bold, HF, MARGIN, y - 30, pageW - 2 * MARGIN, 14, "RAW DATA QUALITY CHECK");
    return y - logoH - 4;
  }

  private static float yellowBanner(
      PDPageContentStream cs,
      PDFont bold,
      float y,
      float x,
      float w,
      String title
  ) throws IOException {
    float h = 15;
    fillRect(cs, x, y - h, w, h, YELLOW);
    strokeRect(cs, x, y - h, w, h);
    centered(cs, bold, BODY, x, y - h, w, h, title);
    return y - h;
  }

  private static float headerPair(
      PDPageContentStream cs,
      PDFont bold,
      float y,
      float x,
      float leftW,
      float rightW,
      String left,
      String right
  ) throws IOException {
    float h = 13;
    strokeRect(cs, x, y - h, leftW, h);
    strokeRect(cs, x + leftW, y - h, rightW, h);
    textAt(cs, bold, BODY, x + 3, y - 10, left);
    centered(cs, bold, BODY, x + leftW, y - h, rightW, h, right);
    return y - h;
  }

  private static float valuePair(
      PDPageContentStream cs,
      PDFont font,
      float y,
      float x,
      float leftW,
      float rightW,
      float h,
      String left,
      String right
  ) throws IOException {
    strokeRect(cs, x, y - h, leftW, h);
    strokeRect(cs, x + leftW, y - h, rightW, h);
    drawWrapped(cs, font, BODY, x + 3, y - 11, leftW - 6, left, h - 4);
    drawWrapped(cs, font, BODY, x + leftW + 4, y - 11, rightW - 8, right, h - 4);
    return y - h;
  }

  private static void drawFooter(PDPageContentStream cs, Fonts fonts, float pageW, String formCode)
      throws IOException {
    float y = 30;
    cs.setStrokingColor(FOOTER_RULE);
    cs.setLineWidth(2.2f);
    cs.moveTo(MARGIN, y + 12);
    cs.lineTo(pageW - MARGIN, y + 12);
    cs.stroke();
    cs.setLineWidth(0.7f);
    cs.setStrokingColor(GRID);

    textAt(cs, fonts.regular, HF, MARGIN, y, formCode);
    textAt(cs, fonts.regular, HF, pageW - MARGIN - 130, y, "Rev 04, date 01/08/2023");
    textAt(cs, fonts.italic, 8, MARGIN, y - 12,
        "This is a controlled document and must be checked against the master documents list for the latest revision level");
  }

  private static PDPage newLandscapePage() {
    return new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
  }

  private static Fonts loadFonts(PDDocument doc) throws IOException {
    PDFont regular = loadEmbedded(doc, "/forms/times.ttf", Standard14Fonts.FontName.TIMES_ROMAN);
    PDFont bold = loadEmbedded(doc, "/forms/timesbd.ttf", Standard14Fonts.FontName.TIMES_BOLD);
    PDFont italic = loadEmbedded(doc, "/forms/timesi.ttf", Standard14Fonts.FontName.TIMES_ITALIC);
    return new Fonts(regular, bold, italic);
  }

private static PDFont loadEmbedded(PDDocument doc, String resource, Standard14Fonts.FontName fallback)
      throws IOException {
    try (InputStream in = Of82PdfExporter.class.getResourceAsStream(resource)) {
      if (in != null) {
        return PDType0Font.load(doc, in, true);
      }
    } catch (IOException ignored) {
      // fall through
    }
    return new PDType1Font(fallback);
  }

  private static PDImageXObject loadLogo(PDDocument doc) throws IOException {
    try (InputStream in = Of82PdfExporter.class.getResourceAsStream("/forms/caa-uganda-logo.png")) {
      if (in == null) {
        return null;
      }
      return PDImageXObject.createFromByteArray(doc, in.readAllBytes(), "caa-logo");
    }
  }

  private static SignatureResponse findSig(List<SignatureResponse> signatures, String key) {
    if (signatures == null) {
      return null;
    }
    for (SignatureResponse s : signatures) {
      if (key.equals(s.slotKey())) {
        return s;
      }
    }
    return null;
  }

  private static Map<String, String> completenessScores(Map<String, Object> payload) {
    Map<String, String> scores = new LinkedHashMap<>();
    if (payload.get("completeness") instanceof List<?> rows) {
      for (Object row : rows) {
        if (row instanceof Map<?, ?> map) {
          scores.put(text(map.get("item")), text(map.get("value")));
        }
      }
    }
    return scores;
  }

  private static String activeColumn(String recordKind, Map<String, Object> payload) {
    if (payload.get("completeness") instanceof List<?> rows && !rows.isEmpty()
        && rows.get(0) instanceof Map<?, ?> map) {
      String column = text(map.get("column"));
      if ("AIC".equalsIgnoreCase(column)) {
        return "AIC";
      }
      if ("AIP".equalsIgnoreCase(column) || "AIP AMDT".equalsIgnoreCase(column)) {
        return "AIP";
      }
      if (!column.isBlank()) {
        return "AIP SUP/NOTAM";
      }
    }
    String upper = recordKind == null ? "" : recordKind.toUpperCase(Locale.ROOT);
    if (upper.contains("AIC")) {
      return "AIC";
    }
    if (upper.contains("AMDT") || (upper.contains("AIP") && !upper.contains("SUP"))) {
      return "AIP";
    }
    return "AIP SUP/NOTAM";
  }

  private static String completenessOverall(Map<String, String> scores) {
    int scored = 0;
    int ok = 0;
    for (String v : scores.values()) {
      if (v == null || v.isBlank() || "N/A".equalsIgnoreCase(v)) {
        continue;
      }
      scored++;
      if (isChecked(v)) {
        ok++;
      }
    }
    return scored > 0 && ok == scored ? "100%" : "";
  }

  private static boolean isHundred(Object value) {
    return isChecked(text(value));
  }

  private static boolean isChecked(String score) {
    return "100%".equals(score) || "Yes".equalsIgnoreCase(score) || "X".equalsIgnoreCase(score);
  }

  private static void fillRect(PDPageContentStream cs, float x, float y, float w, float h, Color color)
      throws IOException {
    cs.setNonStrokingColor(color);
    cs.addRect(x, y, w, h);
    cs.fill();
    cs.setNonStrokingColor(Color.BLACK);
  }

  private static void strokeRect(PDPageContentStream cs, float x, float y, float w, float h)
      throws IOException {
    cs.setStrokingColor(GRID);
    cs.setLineWidth(0.7f);
    cs.addRect(x, y, w, h);
    cs.stroke();
  }

  private static void textAt(PDPageContentStream cs, PDFont font, float size, float x, float y, String value)
      throws IOException {
    cs.beginText();
    cs.setFont(font, size);
    cs.newLineAtOffset(x, y);
    cs.showText(safe(value));
    cs.endText();
  }

  private static void centered(
      PDPageContentStream cs,
      PDFont font,
      float size,
      float x,
      float y,
      float w,
      float h,
      String value
  ) throws IOException {
    float tw = stringWidth(font, size, value);
    textAt(cs, font, size, x + Math.max(2, (w - tw) / 2f), y + h / 2f - size / 3f, value);
  }

  private static void drawWrapped(
      PDPageContentStream cs,
      PDFont font,
      float size,
      float x,
      float y,
      float maxWidth,
      String value,
      float maxHeight
  ) throws IOException {
    List<String> lines = wrap(font, size, value == null || value.isBlank() ? " " : value, maxWidth);
    float cursor = y;
    float minY = y - maxHeight + size;
    for (String line : lines) {
      if (cursor < minY) {
        break;
      }
      textAt(cs, font, size, x, cursor, line);
      cursor -= size + 1.5f;
    }
  }

  private static List<String> wrap(PDFont font, float size, String text, float maxWidth) throws IOException {
    String remaining = text.replace('\n', ' ').trim();
    List<String> lines = new ArrayList<>();
    if (remaining.isEmpty()) {
      lines.add(" ");
      return lines;
    }
    while (!remaining.isEmpty() && lines.size() < 10) {
      int fit = remaining.length();
      while (fit > 0 && stringWidth(font, size, remaining.substring(0, fit)) > maxWidth) {
        fit--;
      }
      if (fit <= 0) {
        fit = Math.min(1, remaining.length());
      }
      if (fit < remaining.length()) {
        int space = remaining.lastIndexOf(' ', fit);
        if (space > 6) {
          fit = space;
        }
      }
      lines.add(remaining.substring(0, fit).trim());
      remaining = remaining.substring(fit).trim();
    }
    return lines;
  }

  private static float stringWidth(PDFont font, float size, String text) throws IOException {
    return font.getStringWidth(safe(text)) / 1000f * size;
  }

  private static String text(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }

  private static String nullSafe(String value) {
    return value == null ? "" : value;
  }

  private static String truncate(String value, int max) {
    if (value.length() <= max) {
      return value;
    }
    return value.substring(0, Math.max(0, max - 1)) + "...";
  }

  private static String safe(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '\t') {
        sb.append(' ');
      } else if (c >= 32 && c <= 126 || c == 160 || (c >= 192 && c <= 255)) {
        sb.append(c);
      } else if (c == '–' || c == '—' || c == '−') {
        sb.append('-');
      } else if (c == '‘' || c == '’') {
        sb.append('\'');
      } else if (c == '“' || c == '”') {
        sb.append('"');
      } else if (c == '…') {
        sb.append("...");
      } else {
        sb.append('?');
      }
    }
    return sb.toString();
  }

  private record Fonts(PDFont regular, PDFont bold, PDFont italic) {
  }
}
