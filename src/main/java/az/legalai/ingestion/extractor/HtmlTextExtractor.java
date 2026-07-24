package az.legalai.ingestion.extractor;

import az.legalai.ingestion.cleaner.TextBlock;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class HtmlTextExtractor implements DocumentTextExtractor {
    public boolean supports(String mime, String name) {
        return "text/html".equals(mime) || name.toLowerCase().matches(".*\\.html?$");
    }

    public ExtractedDocument extract(InputStream in) {
        try {
            var doc = Jsoup.parse(in, StandardCharsets.UTF_8.name(), "");
            doc.select("script,style,nav,aside").remove();
            List<TextBlock> b = new ArrayList<>();
            int i = 0;
            for (Element e : doc.select("h1,h2,h3,h4,h5,h6,p,li,td")) {
                String t = e.text().trim();
                if (!t.isBlank())
                    b.add(
                            new TextBlock(
                                    i++,
                                    t,
                                    e.tagName().startsWith("h") ? "HEADING" : "PARAGRAPH",
                                    null,
                                    e.tagName(),
                                    e.tagName().startsWith("h"),
                                    null));
            }
            return new ExtractedDocument(doc.text(), b, Map.of("title", doc.title()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
