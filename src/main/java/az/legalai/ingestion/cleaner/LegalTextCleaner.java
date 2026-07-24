package az.legalai.ingestion.cleaner;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class LegalTextCleaner {
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cc}&&[^\\r\\n\\t]]");
    private static final Pattern SPACE = Pattern.compile("[\\s\\u00a0]+");
    private static final Pattern PAGE =
            Pattern.compile("(?iu)^(?:page|səhifə|страница)?\\s*\\d+(?:\\s*/\\s*\\d+)?$");

    public List<TextBlock> clean(List<TextBlock> input) {
        Map<String, Integer> repeatedHeaders = new HashMap<>();
        for (TextBlock block : input) {
            String normalized = normalize(block.text());
            if (isHeader(block) && normalized.length() <= 180) {
                repeatedHeaders.merge(normalized.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }
        List<TextBlock> result = new ArrayList<>();
        for (TextBlock block : input) {
            String text = normalize(block.text());
            if (text.isBlank() || PAGE.matcher(text).matches()) continue;
            if (isHeader(block)
                    && repeatedHeaders.getOrDefault(text.toLowerCase(Locale.ROOT), 0) > 1) continue;
            result.add(
                    new TextBlock(
                            block.orderIndex(),
                            text,
                            block.blockType(),
                            block.pageNumber(),
                            block.styleName(),
                            block.bold(),
                            block.fontSize()));
        }
        return List.copyOf(result);
    }

    private boolean isHeader(TextBlock block) {
        return "HEADER".equalsIgnoreCase(block.blockType())
                || "FOOTER".equalsIgnoreCase(block.blockType());
    }

    private String normalize(String input) {
        if (input == null) return "";
        String text =
                Normalizer.normalize(input, Normalizer.Form.NFC)
                        .replace("\u00AD", "")
                        .replace('\u2010', '-')
                        .replace('\u2011', '-');
        text = CONTROL.matcher(text).replaceAll("");
        return SPACE.matcher(text).replaceAll(" ").trim();
    }
}
