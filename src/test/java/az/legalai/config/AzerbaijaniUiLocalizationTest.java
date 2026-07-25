package az.legalai.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class AzerbaijaniUiLocalizationTest {
    private static final Pattern CYRILLIC = Pattern.compile("[\\p{IsCyrillic}]");
    private static final List<String> UI_RESOURCES =
            List.of(
                    "templates/login.html",
                    "templates/dashboard.html",
                    "templates/fragments.html",
                    "templates/documents/list.html",
                    "templates/documents/upload.html",
                    "templates/documents/details.html",
                    "templates/search.html",
                    "static/js/stt.js");

    @Test
    void visibleUiResourcesContainNoCyrillicText() throws IOException {
        for (String resource : UI_RESOURCES) {
            String content = read(resource);
            assertThat(CYRILLIC.matcher(content).find())
                    .as("%s daxilində kiril mətni olmamalıdır", resource)
                    .isFalse();
        }
    }

    @Test
    void everyFullPageDeclaresAzerbaijaniLanguage() throws IOException {
        for (String resource :
                UI_RESOURCES.stream()
                        .filter(path -> path.endsWith(".html") && !path.endsWith("fragments.html"))
                        .toList()) {
            assertThat(read(resource)).as(resource).contains("<html lang=\"az\"");
        }
    }

    private String read(String resource) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(stream).as(resource).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
