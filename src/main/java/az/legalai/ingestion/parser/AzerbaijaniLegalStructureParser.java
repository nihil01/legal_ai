package az.legalai.ingestion.parser;

import az.legalai.ingestion.cleaner.TextBlock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AzerbaijaniLegalStructureParser implements LegalStructureParser {
    private static final Pattern ARTICLE =
            Pattern.compile("(?iu)^(?:maddə|статья)\\s+([0-9]+(?:-[0-9]+)?)\\.?\\s*(.*)$");
    private static final Pattern CHAPTER =
            Pattern.compile(
                    "(?iu)^(?:(\\d+(?:-(?:ci|cı|cu|cü))?|[IVXLCDM]+)\\s+)?(?:fəsil|глава)\\s*\\.?\\s*(.*)$");
    private static final Pattern SECTION =
            Pattern.compile(
                    "(?iu)^(?:(\\d+|[IVXLCDM]+|birinci|ikinci|üçüncü|dördüncü)\\s+)?(?:bölmə|раздел)\\s*\\.?\\s*(.*)$");
    private static final Pattern PART =
            Pattern.compile(
                    "(?iu)^(?:(\\d+|[IVXLCDM]+|birinci|ikinci|первая|вторая)\\s+)?(?:hissə|часть)\\s*\\.?\\s*(.*)$");
    private static final Pattern NUMBERED =
            Pattern.compile("^([0-9]+(?:\\.[0-9]+){1,})\\.?\\s*(.*)$");

    @Override
    public LegalSection parse(String documentTitle, List<TextBlock> blocks) {
        Node root = new Node(SectionType.DOCUMENT, null, documentTitle, "");
        Map<SectionType, Node> current = new EnumMap<>(SectionType.class);
        current.put(SectionType.DOCUMENT, root);
        for (TextBlock block : blocks) {
            Parsed parsed = classify(block.text());
            if (parsed.type == SectionType.UNKNOWN) {
                Node parent =
                        nearest(
                                current,
                                SectionType.SUBCLAUSE,
                                SectionType.CLAUSE,
                                SectionType.ARTICLE,
                                SectionType.CHAPTER,
                                SectionType.SECTION,
                                SectionType.PART,
                                SectionType.DOCUMENT);
                parent.append(block.text());
                continue;
            }
            Node node = new Node(parsed.type, parsed.number, parsed.title, parsed.body);
            Node parent =
                    switch (parsed.type) {
                        case PART -> root;
                        case SECTION -> nearest(current, SectionType.PART, SectionType.DOCUMENT);
                        case CHAPTER ->
                                nearest(
                                        current,
                                        SectionType.SECTION,
                                        SectionType.PART,
                                        SectionType.DOCUMENT);
                        case ARTICLE ->
                                nearest(
                                        current,
                                        SectionType.CHAPTER,
                                        SectionType.SECTION,
                                        SectionType.PART,
                                        SectionType.DOCUMENT);
                        case CLAUSE, SUBCLAUSE ->
                                nearest(
                                        current,
                                        SectionType.ARTICLE,
                                        SectionType.CHAPTER,
                                        SectionType.DOCUMENT);
                        default -> root;
                    };
            parent.children.add(node);
            current.put(parsed.type, node);
            clearLower(current, parsed.type);
        }
        return root.freeze();
    }

    private Parsed classify(String text) {
        Matcher m = ARTICLE.matcher(text);
        if (m.matches()) return new Parsed(SectionType.ARTICLE, m.group(1), m.group(2), "");
        m = CHAPTER.matcher(text);
        if (m.matches()) return new Parsed(SectionType.CHAPTER, m.group(1), m.group(2), "");
        m = SECTION.matcher(text);
        if (m.matches()) return new Parsed(SectionType.SECTION, m.group(1), m.group(2), "");
        m = PART.matcher(text);
        if (m.matches()) return new Parsed(SectionType.PART, m.group(1), m.group(2), "");
        m = NUMBERED.matcher(text);
        if (m.matches()) {
            String number = m.group(1);
            SectionType type =
                    number.chars().filter(ch -> ch == '.').count() >= 2
                            ? SectionType.SUBCLAUSE
                            : SectionType.CLAUSE;
            return new Parsed(type, number, "", m.group(2));
        }
        return new Parsed(SectionType.UNKNOWN, null, "", text);
    }

    private Node nearest(Map<SectionType, Node> current, SectionType... types) {
        for (SectionType type : types) if (current.containsKey(type)) return current.get(type);
        throw new IllegalStateException("Document root is missing");
    }

    private void clearLower(Map<SectionType, Node> current, SectionType type) {
        int level = level(type);
        current.keySet().removeIf(candidate -> level(candidate) > level);
    }

    private int level(SectionType type) {
        return switch (type) {
            case DOCUMENT -> 0;
            case PART -> 1;
            case SECTION -> 2;
            case CHAPTER -> 3;
            case ARTICLE -> 4;
            case PARAGRAPH, CLAUSE -> 5;
            case SUBCLAUSE -> 6;
            case UNKNOWN -> 7;
        };
    }

    private record Parsed(SectionType type, String number, String title, String body) {}

    private static final class Node {
        private final SectionType type;
        private final String number;
        private final String title;
        private String text;
        private final List<Node> children = new ArrayList<>();

        private Node(SectionType type, String number, String title, String text) {
            this.type = type;
            this.number = number;
            this.title = title;
            this.text = text;
        }

        private void append(String value) {
            text = text.isBlank() ? value : text + "\n" + value;
        }

        private LegalSection freeze() {
            return new LegalSection(
                    type, number, title, text, children.stream().map(Node::freeze).toList());
        }
    }
}
