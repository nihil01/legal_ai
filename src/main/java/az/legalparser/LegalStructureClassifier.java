package az.legalparser;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegalStructureClassifier {
    private static final Pattern ARTICLE = Pattern.compile(
            "^Maddə\\s+([0-9]+(?:-[0-9]+)?)\\.?\\s*(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NUMBERED = Pattern.compile(
            "^([0-9]+(?:[.-][0-9]+)+(?:-[0-9]+)?)\\.?\\s+(.*)$");
    private static final Pattern CHAPTER = Pattern.compile(
            "^(?:[0-9]+-(?:ci|cı|cu|cü)|[IVXLCDM]+)\\s+fəsil\\.?\\s*(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SECTION = Pattern.compile(
            "^(?:Birinci|İkinci|Üçüncü|Dördüncü|Beşinci|Altıncı|Yeddinci|Səkkizinci|Doqquzuncu|Onuncu|[0-9]+-(?:ci|cı|cu|cü))\\s+bölmə\\b.*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern PART = Pattern.compile(
            "^(?:ÜMUMİ|XÜSUSİ|BİRİNCİ|İKİNCİ|ÜÇÜNCÜ|DÖRDÜNCÜ|BEŞİNCİ).*(?:HİSSƏ|HISSƏ)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public Classification classify(String text, int index) {
        Matcher article = ARTICLE.matcher(text);
        if (article.matches()) {
            return new Classification(ParagraphKind.ARTICLE, article.group(1));
        }

        Matcher numbered = NUMBERED.matcher(text);
        if (numbered.matches()) {
            return new Classification(ParagraphKind.NUMBERED_PARAGRAPH, numbered.group(1));
        }

        Matcher chapter = CHAPTER.matcher(text);
        if (chapter.matches()) {
            return new Classification(ParagraphKind.CHAPTER, extractLeadingNumber(text));
        }

        if (SECTION.matcher(text).matches()) {
            return new Classification(ParagraphKind.SECTION, null);
        }
        if (PART.matcher(text).matches()) {
            return new Classification(ParagraphKind.PART, null);
        }

        String upper = text.toUpperCase(Locale.forLanguageTag("az"));
        if (index < 10 && text.length() <= 180 && upper.equals(text)) {
            return new Classification(ParagraphKind.TITLE, null);
        }
        return new Classification(ParagraphKind.BODY, null);
    }

    private String extractLeadingNumber(String text) {
        int space = text.indexOf(' ');
        return space > 0 ? text.substring(0, space) : null;
    }

    public record Classification(ParagraphKind kind, String number) {
    }
}
