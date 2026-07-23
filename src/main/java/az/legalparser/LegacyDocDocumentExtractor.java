package az.legalparser;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LegacyDocDocumentExtractor implements WordDocumentExtractor {
    @Override
    public ExtractionResult extract(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file);
             HWPFDocument document = new HWPFDocument(input)) {
            Range range = document.getRange();
            List<RawParagraph> paragraphs = new ArrayList<>();
            for (int i = 0; i < range.numParagraphs(); i++) {
                Paragraph paragraph = range.getParagraph(i);
                paragraphs.add(new RawParagraph(paragraph.text(), false, false, List.of()));
            }
            return new ExtractionResult(range.numParagraphs(), paragraphs);
        }
    }
}
