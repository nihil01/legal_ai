package az.legalparser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LegalDocumentParser {
    private final DocumentFormatDetector formatDetector = new DocumentFormatDetector();
    private final TextCleaner cleaner = new TextCleaner();
    private final LegalStructureClassifier classifier = new LegalStructureClassifier();

    public ParsedDocument parse(Path file) throws IOException {
        DocumentFormat format = formatDetector.detect(file);
        WordDocumentExtractor extractor = switch (format) {
            case DOCX -> new DocxDocumentExtractor();
            case LEGACY_DOC -> new LegacyDocDocumentExtractor();
            case UNKNOWN -> throw new IOException("Unsupported file format: " + file);
        };

        WordDocumentExtractor.ExtractionResult extracted = extractor.extract(file);
        List<ParsedParagraph> parsed = new ArrayList<>();
        String title = null;

        for (int sourceIndex = 0; sourceIndex < extracted.paragraphs().size(); sourceIndex++) {
            WordDocumentExtractor.RawParagraph raw = extracted.paragraphs().get(sourceIndex);
            String text = cleaner.clean(raw.text());
            if (text.isBlank()) {
                continue;
            }

            var classification = classifier.classify(text, parsed.size());
            if (title == null) {
                title = text;
            }

            parsed.add(new ParsedParagraph(
                    sourceIndex,
                    classification.kind(),
                    classification.number(),
                    text,
                    raw.fullyStruck(),
                    raw.partiallyStruck(),
                    raw.endnoteReferences()
            ));
        }

        return new ParsedDocument(
                file.getFileName().toString(),
                format,
                title,
                extracted.sourceParagraphCount(),
                parsed.size(),
                List.copyOf(parsed)
        );
    }
}
