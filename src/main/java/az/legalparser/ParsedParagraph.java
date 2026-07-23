package az.legalparser;

import java.util.List;

public record ParsedParagraph(
        int index,
        ParagraphKind kind,
        String number,
        String text,
        boolean fullyStruck,
        boolean partiallyStruck,
        List<String> endnoteReferences
) {
}
