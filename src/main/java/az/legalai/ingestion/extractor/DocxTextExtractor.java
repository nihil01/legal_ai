package az.legalai.ingestion.extractor;

import az.legalai.ingestion.cleaner.TextBlock;
import java.io.*;
import java.util.*;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

@Component
public class DocxTextExtractor implements DocumentTextExtractor {
    public boolean supports(String mime, String name) {
        return mime.contains("officedocument.wordprocessingml")
                || name.toLowerCase().endsWith(".docx");
    }

    public ExtractedDocument extract(InputStream in) {
        try (XWPFDocument d = new XWPFDocument(in)) {
            List<TextBlock> b = new ArrayList<>();
            StringBuilder raw = new StringBuilder();
            int idx = 0;
            for (IBodyElement element : d.getBodyElements()) {
                if (element instanceof XWPFParagraph p) {
                    String t = p.getText();
                    boolean bold = p.getRuns().stream().anyMatch(XWPFRun::isBold);
                    Double size =
                            p.getRuns().stream()
                                    .map(XWPFRun::getFontSizeAsDouble)
                                    .filter(Objects::nonNull)
                                    .max(Double::compareTo)
                                    .orElse(null);
                    b.add(new TextBlock(idx++, t, "PARAGRAPH", null, p.getStyle(), bold, size));
                    raw.append(t).append('\n');
                } else if (element instanceof XWPFTable table) {
                    for (XWPFTableRow row : table.getRows()) {
                        String t =
                                row.getTableCells().stream()
                                        .map(XWPFTableCell::getText)
                                        .reduce((a, c) -> a + " | " + c)
                                        .orElse("");
                        b.add(new TextBlock(idx++, t, "TABLE", null, null, false, null));
                        raw.append(t).append('\n');
                    }
                }
            }
            return new ExtractedDocument(raw.toString(), b, Map.of());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
