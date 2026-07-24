package az.legalai.ingestion.parser;

import java.util.List;

public record LegalSection(
        SectionType type, String number, String title, String text, List<LegalSection> children) {
    public LegalSection {
        children = children == null ? List.of() : List.copyOf(children);
        text = text == null ? "" : text;
        title = title == null ? "" : title;
    }
}
