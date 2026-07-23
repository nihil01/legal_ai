package az.legalparser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface WordDocumentExtractor {
    ExtractionResult extract(Path file) throws IOException;

    record RawParagraph(
            String text,
            boolean fullyStruck,
            boolean partiallyStruck,
            List<String> endnoteReferences
    ) {
    }

    record ExtractionResult(int sourceParagraphCount, List<RawParagraph> paragraphs) {
    }
}
