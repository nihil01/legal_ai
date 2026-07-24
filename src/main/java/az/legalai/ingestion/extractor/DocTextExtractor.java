package az.legalai.ingestion.extractor;

import az.legalai.ingestion.cleaner.TextBlock;
import java.io.*;
import java.util.*;
import org.apache.poi.hwpf.HWPFDocument;
import org.springframework.stereotype.Component;

@Component
public class DocTextExtractor implements DocumentTextExtractor {
    public boolean supports(String mime, String name) {
        return "application/msword".equals(mime) || name.toLowerCase().endsWith(".doc");
    }

    public ExtractedDocument extract(InputStream in) {
        try (HWPFDocument d = new HWPFDocument(in)) {
            var range = d.getRange();
            List<TextBlock> b = new ArrayList<>();
            StringBuilder raw = new StringBuilder();
            for (int i = 0; i < range.numParagraphs(); i++) {
                String t = range.getParagraph(i).text();
                b.add(new TextBlock(i, t, "PARAGRAPH", null, null, false, null));
                raw.append(t).append('\n');
            }
            return new ExtractedDocument(raw.toString(), b, Map.of());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
