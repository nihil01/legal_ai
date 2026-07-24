package az.legalai.ingestion.extractor;

import az.legalai.ingestion.cleaner.TextBlock;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class PlainTextExtractor implements DocumentTextExtractor {
    public boolean supports(String mime, String name) {
        return "text/plain".equals(mime) || name.toLowerCase().endsWith(".txt");
    }

    public ExtractedDocument extract(InputStream in) {
        try {
            String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String[] lines = raw.split("\\R");
            List<TextBlock> b = new ArrayList<>();
            for (int i = 0; i < lines.length; i++)
                b.add(new TextBlock(i, lines[i], "PARAGRAPH", null, null, false, null));
            return new ExtractedDocument(raw, b, Map.of());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
