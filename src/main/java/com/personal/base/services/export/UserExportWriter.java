package com.personal.base.services.export;

import com.personal.base.models.ERole;
import com.personal.base.models.Role;
import com.personal.base.models.User;
import com.personal.base.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Collectors;

// Reads users page by page ("đọc DB theo batch/stream") and writes them straight into
// the given OutputStream (the HTTP response body — nothing is written to disk).
@Component
public class UserExportWriter {

  private static final int BATCH_SIZE = 500;
  private static final String[] HEADERS = {
          "ID", "Username", "Email", "Phone", "Date of birth", "Address", "Roles"
  };

  @Autowired
  private UserRepository userRepository;

  // readOnly keeps a single Hibernate session open for the whole batch loop below,
  // so accessing the lazy `roles` collection per user doesn't blow up outside a session.
  @Transactional(readOnly = true)
  public int writeExcel(ERole role, String keyword, OutputStream out) throws IOException {
    try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
      Sheet sheet = workbook.createSheet("Users");

      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < HEADERS.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(HEADERS[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowNum = 1;
      int pageNum = 0;
      Page<User> batch;
      do {
        batch = userRepository.searchForExport(role, keyword, PageRequest.of(pageNum, BATCH_SIZE));
        for (User u : batch.getContent()) {
          String[] values = toRowValues(u);
          Row row = sheet.createRow(rowNum++);
          for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
          }
        }
        pageNum++;
      } while (!batch.isLast());

      workbook.write(out);
      return rowNum - 1;
    }
  }

  @Transactional(readOnly = true)
  public int writeWord(ERole role, String keyword, OutputStream out) throws IOException {
    try (XWPFDocument document = new XWPFDocument()) {
      XWPFParagraph title = document.createParagraph();
      title.setAlignment(ParagraphAlignment.CENTER);
      XWPFRun titleRun = title.createRun();
      titleRun.setText("Danh sach nguoi dung");
      titleRun.setBold(true);
      titleRun.setFontSize(16);

      XWPFTable table = document.createTable(1, HEADERS.length);
      XWPFTableRow headerRow = table.getRow(0);
      for (int i = 0; i < HEADERS.length; i++) {
        headerRow.getCell(i).setText(HEADERS[i]);
      }

      int rowNum = 0;
      int pageNum = 0;
      Page<User> batch;
      do {
        batch = userRepository.searchForExport(role, keyword, PageRequest.of(pageNum, BATCH_SIZE));
        for (User u : batch.getContent()) {
          String[] values = toRowValues(u);
          XWPFTableRow row = table.createRow();
          for (int i = 0; i < values.length; i++) {
            row.getCell(i).setText(values[i]);
          }
          rowNum++;
        }
        pageNum++;
      } while (!batch.isLast());

      document.write(out);
      return rowNum;
    }
  }

  private static final float[] PDF_COLUMN_WIDTHS = {35, 90, 170, 75, 65, 130, 110};
  private static final float PDF_MARGIN = 30;
  private static final float PDF_ROW_HEIGHT = 16;
  private static final float PDF_FONT_SIZE = 9;
  private static final float PDF_HEADER_FONT_SIZE = 10;

  @Transactional(readOnly = true)
  public int writePdf(ERole role, String keyword, OutputStream out) throws IOException {
    try (PDDocument document = new PDDocument()) {
      // Noto Sans has full Vietnamese coverage (unlike the Roboto subset bundled with
      // some JS PDF libs, which drops the precomposed tone-mark letters).
      PDFont font = PDType0Font.load(document, new ClassPathResource("fonts/NotoSans-Regular.ttf").getInputStream());
      PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

      PdfCursor cursor = new PdfCursor(document, pageSize, font);
      cursor.newPage();
      cursor.drawRow(HEADERS, PDF_HEADER_FONT_SIZE, true);

      int rowNum = 0;
      int pageNum = 0;
      Page<User> batch;
      do {
        batch = userRepository.searchForExport(role, keyword, PageRequest.of(pageNum, BATCH_SIZE));
        for (User u : batch.getContent()) {
          if (cursor.needsNewPage()) {
            cursor.newPage();
            cursor.drawRow(HEADERS, PDF_HEADER_FONT_SIZE, true);
          }
          cursor.drawRow(toRowValues(u), PDF_FONT_SIZE, false);
          rowNum++;
        }
        pageNum++;
      } while (!batch.isLast());

      cursor.close();
      document.save(out);
      return rowNum;
    }
  }

  private String[] toRowValues(User u) {
    String roles = u.getRoles().stream().map(Role::getName).map(ERole::name).collect(Collectors.joining(", "));
    return new String[]{
            String.valueOf(u.getId()),
            nullToEmpty(u.getUsername()),
            nullToEmpty(u.getEmail()),
            nullToEmpty(u.getPhoneNumber()),
            u.getDateOfBirth() == null ? "" : u.getDateOfBirth().toString(),
            nullToEmpty(u.getAddress()),
            roles
    };
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  // Walks a PDDocument page by page, drawing fixed-width table rows and opening a new
  // page whenever the current one runs out of vertical space.
  private static final class PdfCursor {
    private final PDDocument document;
    private final PDRectangle pageSize;
    private final PDFont font;
    private PDPageContentStream contentStream;
    private float y;

    PdfCursor(PDDocument document, PDRectangle pageSize, PDFont font) {
      this.document = document;
      this.pageSize = pageSize;
      this.font = font;
    }

    boolean needsNewPage() {
      return y < PDF_MARGIN + PDF_ROW_HEIGHT;
    }

    void newPage() throws IOException {
      if (contentStream != null) {
        contentStream.close();
      }
      PDPage page = new PDPage(pageSize);
      document.addPage(page);
      contentStream = new PDPageContentStream(document, page);
      y = pageSize.getHeight() - PDF_MARGIN;
    }

    void drawRow(String[] values, float fontSize, boolean header) throws IOException {
      float x = PDF_MARGIN;
      for (int i = 0; i < values.length; i++) {
        float width = PDF_COLUMN_WIDTHS[i];
        String text = truncateToWidth(values[i], fontSize, width - 4);
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        x += width;
      }
      y -= PDF_ROW_HEIGHT;

      if (header) {
        contentStream.setLineWidth(0.75f);
        contentStream.moveTo(PDF_MARGIN, y + PDF_ROW_HEIGHT - 4);
        contentStream.lineTo(pageSize.getWidth() - PDF_MARGIN, y + PDF_ROW_HEIGHT - 4);
        contentStream.stroke();
      }
    }

    private String truncateToWidth(String text, float fontSize, float maxWidth) throws IOException {
      if (text.isEmpty()) return text;
      if (stringWidth(text, fontSize) <= maxWidth) return text;

      String ellipsis = "...";
      StringBuilder sb = new StringBuilder();
      for (char c : text.toCharArray()) {
        String candidate = sb.toString() + c + ellipsis;
        if (stringWidth(candidate, fontSize) > maxWidth) break;
        sb.append(c);
      }
      return sb + ellipsis;
    }

    private float stringWidth(String text, float fontSize) throws IOException {
      return font.getStringWidth(text) / 1000 * fontSize;
    }

    void close() throws IOException {
      if (contentStream != null) {
        contentStream.close();
      }
    }
  }
}
