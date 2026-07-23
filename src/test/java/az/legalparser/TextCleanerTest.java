package az.legalparser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextCleanerTest {
    private final TextCleaner cleaner = new TextCleaner();

    @Test
    void normalizesWhitespaceAndSoftHyphen() {
        assertEquals("Maddə 1. Test", cleaner.clean("  Maddə\u00A01.\u00AD  Test  "));
    }
}
