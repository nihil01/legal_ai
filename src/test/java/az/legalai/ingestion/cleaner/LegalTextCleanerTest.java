package az.legalai.ingestion.cleaner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LegalTextCleanerTest {
    private final LegalTextCleaner cleaner = new LegalTextCleaner();

    @Test
    void removesNoiseWithoutDestroyingLegalNumbering() {
        var blocks =
                List.of(
                        new TextBlock(
                                0,
                                "  Maddə\u00A010. Fiziki şəxslərin hüquq qabiliyyəti  ",
                                "PARAGRAPH",
                                null,
                                "Heading 2",
                                true,
                                14.0),
                        new TextBlock(1, "Page 12 / 44", "PARAGRAPH", 12, null, false, 10.0),
                        new TextBlock(
                                2,
                                "10.1.  Fiziki şəxsin hüquq qabiliyyəti onun doğulduğu anda yaranır.\u0007",
                                "PARAGRAPH",
                                12,
                                null,
                                false,
                                11.0));

        var cleaned = cleaner.clean(blocks);

        assertThat(cleaned)
                .extracting(TextBlock::text)
                .containsExactly(
                        "Maddə 10. Fiziki şəxslərin hüquq qabiliyyəti",
                        "10.1. Fiziki şəxsin hüquq qabiliyyəti onun doğulduğu anda yaranır.");
        assertThat(cleaned.getFirst().bold()).isTrue();
    }

    @Test
    void removesRepeatedShortHeadersButKeepsRepeatedLegalClauses() {
        var blocks =
                List.of(
                        new TextBlock(
                                0,
                                "AZƏRBAYCAN RESPUBLİKASININ QANUNU",
                                "HEADER",
                                1,
                                null,
                                true,
                                9.0),
                        new TextBlock(1, "1.1. Hüquq norması.", "PARAGRAPH", 1, null, false, 11.0),
                        new TextBlock(
                                2,
                                "AZƏRBAYCAN RESPUBLİKASININ QANUNU",
                                "HEADER",
                                2,
                                null,
                                true,
                                9.0),
                        new TextBlock(3, "1.1. Hüquq norması.", "PARAGRAPH", 2, null, false, 11.0));

        assertThat(cleaner.clean(blocks))
                .extracting(TextBlock::text)
                .containsExactly("1.1. Hüquq norması.", "1.1. Hüquq norması.");
    }
}
