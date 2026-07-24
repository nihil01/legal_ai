package az.legalai.ingestion.extractor;

import az.legalai.ingestion.cleaner.TextBlock;
import java.io.*;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfTextExtractor implements DocumentTextExtractor {
    public boolean supports(String mime, String name) {
        return "application/pdf".equals(mime) || name.toLowerCase().endsWith(".pdf");
    }

    public ExtractedDocument extract(InputStream in) {
        try (var doc = Loader.loadPDF(in.readAllBytes())) {
            List<TextBlock> b = new ArrayList<>();
            StringBuilder raw = new StringBuilder();
            int idx = 0;
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                PDFTextStripper s = new PDFTextStripper();
                s.setStartPage(p);
                s.setEndPage(p);
                String text = s.getText(doc);
                raw.append(text);
                for (String line : text.split("\\R"))
                    b.add(new TextBlock(idx++, line, "PARAGRAPH", p, null, false, null));
            }
            return new ExtractedDocument(
                    raw.toString(), b, Map.of("pages", String.valueOf(doc.getNumberOfPages())));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
