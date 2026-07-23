package az.legalparser;

import java.util.List;

public record ParsedDocument(
        String sourceFile,
        DocumentFormat detectedFormat,
        String title,
        int sourceParagraphCount,
        int outputParagraphCount,
        List<ParsedParagraph> paragraphs
) {
}
