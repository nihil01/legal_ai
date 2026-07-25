package az.legalai.document.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UploadCommandTest {
    @Test
    void rejectsNonHttpSourceUrl() {
        assertThatThrownBy(
                        () ->
                                new UploadCommand(
                                        "Law", null, "javascript:alert(1)", null, null, "az"))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("URL");
    }

    @Test
    void rejectsInvalidLanguageTag() {
        assertThatThrownBy(() -> new UploadCommand("Law", null, null, null, null, "az<script>"))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("Dil kodu");
    }
}
