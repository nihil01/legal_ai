package az.legalai.ingestion.chunker;

import az.legalai.ingestion.parser.LegalSection;
import az.legalai.ingestion.parser.SectionType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LegalDocumentChunker {
    private final int maxCharacters;

    public LegalDocumentChunker(int maxCharacters) {
        if (maxCharacters < 100) throw new IllegalArgumentException("maxCharacters must be >= 100");
        this.maxCharacters = maxCharacters;
    }

    public List<ChunkDraft> chunk(LegalSection root) {
        List<ChunkDraft> result = new ArrayList<>();
        walk(
                root,
                new Context(root.title(), null, null, null, new ArrayList<>(List.of(root.title()))),
                result);
        return List.copyOf(result);
    }

    private void walk(LegalSection section, Context context, List<ChunkDraft> out) {
        Context next = context.with(section);
        if (section.type() == SectionType.ARTICLE) {
            String combined = render(section);
            if (combined.length() <= maxCharacters / 2 || section.children().isEmpty())
                addSplit(section, next, combined, out);
            else
                for (LegalSection child : section.children())
                    addSplit(child, next.with(child), render(child), out);
            return;
        }
        for (LegalSection child : section.children()) walk(child, next, out);
    }

    private void addSplit(
            LegalSection section, Context context, String content, List<ChunkDraft> out) {
        if (content.isBlank()) return;
        List<String> parts = split(content);
        for (String part : parts) {
            int index = out.size();
            String path = String.join(" > ", context.path);
            String embedding = "Sənəd: " + context.documentTitle + "\n" + path + "\n\n" + part;
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("article", context.article);
            if (context.clause != null) metadata.put("clause", context.clause);
            out.add(
                    new ChunkDraft(
                            index,
                            section.type(),
                            section.number(),
                            section.title(),
                            context.article,
                            context.clause,
                            path,
                            part,
                            embedding,
                            estimateTokens(embedding),
                            Map.copyOf(metadata)));
        }
    }

    private List<String> split(String text) {
        if (text.length() <= maxCharacters) return List.of(text);
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxCharacters, text.length());
            if (end < text.length()) {
                int boundary = text.lastIndexOf(' ', end);
                if (boundary > start + maxCharacters / 2) end = boundary;
            }
            parts.add(text.substring(start, end).trim());
            start = end;
            while (start < text.length() && Character.isWhitespace(text.charAt(start))) start++;
        }
        return parts;
    }

    private String render(LegalSection section) {
        StringBuilder b = new StringBuilder();
        appendSection(b, section);
        for (LegalSection child : section.children()) appendSection(b, child);
        return b.toString().trim();
    }

    private void appendSection(StringBuilder b, LegalSection s) {
        if (s.number() != null) b.append(s.number()).append(". ");
        if (!s.title().isBlank()) b.append(s.title());
        if (!s.text().isBlank()) {
            if (!b.isEmpty()) b.append(' ');
            b.append(s.text());
        }
        b.append('\n');
    }

    private int estimateTokens(String text) {
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private record Context(
            String documentTitle,
            String article,
            String clause,
            String chapter,
            List<String> path) {
        Context with(LegalSection s) {
            String a = article, c = clause, ch = chapter;
            List<String> p = new ArrayList<>(path);
            String label =
                    switch (s.type()) {
                        case PART -> "Hissə";
                        case SECTION -> "Bölmə";
                        case CHAPTER -> "Fəsil";
                        case ARTICLE -> "Maddə";
                        case PARAGRAPH -> "Abzas";
                        case CLAUSE, SUBCLAUSE -> "Bənd";
                        default -> s.type().name();
                    };
            if (s.type() != SectionType.DOCUMENT && (s.number() != null || !s.title().isBlank()))
                p.add(
                        label
                                + " "
                                + (s.number() == null ? "" : s.number())
                                + (s.title().isBlank() ? "" : " " + s.title()));
            if (s.type() == SectionType.ARTICLE) a = s.number();
            if (s.type() == SectionType.CLAUSE || s.type() == SectionType.SUBCLAUSE) c = s.number();
            if (s.type() == SectionType.CHAPTER) ch = s.number();
            return new Context(documentTitle, a, c, ch, p);
        }
    }
}
