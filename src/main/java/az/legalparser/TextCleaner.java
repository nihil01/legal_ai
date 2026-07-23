package az.legalparser;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class TextCleaner {
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00A0]+");
    private static final Pattern SOFT_HYPHEN = Pattern.compile("\\u00AD");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cc}&&[^\\r\\n\\t]]");
    private static final Pattern PAGE_MARKER = Pattern.compile("(?i)^page\\s+\\d+(?:\\s*/\\s*\\d+)?$");

    public String clean(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String text = Normalizer.normalize(input, Normalizer.Form.NFC);
        text = SOFT_HYPHEN.matcher(text).replaceAll("");
        text = CONTROL.matcher(text).replaceAll("");
        text = text.replace('\u2010', '-').replace('\u2011', '-');
        text = WHITESPACE.matcher(text).replaceAll(" ").trim();
        return PAGE_MARKER.matcher(text).matches() ? "" : text;
    }
}
