package az.legalai.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DocumentValidatorTest {
    private final DocumentValidator validator = new DocumentValidator(1024);

    @Test
    void calculatesSha256AndNormalizesDetectedTextMimeType() {
        var file =
                new MockMultipartFile(
                        "file", "law.txt", "application/octet-stream", "Maddə 1".getBytes());

        var result = validator.validate(file);

        assertThat(result.mimeType()).isEqualTo("text/plain");
        assertThat(result.checksum()).hasSize(64);
        assertThat(result.bytes()).containsExactly("Maddə 1".getBytes());
    }

    @Test
    void rejectsEmptyOversizedAndUnsupportedFiles() {
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        new MockMultipartFile(
                                                "file", "empty.txt", "text/plain", new byte[0])))
                .isInstanceOf(DocumentValidationException.class);
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        new MockMultipartFile(
                                                "file", "large.txt", "text/plain", new byte[1025])))
                .isInstanceOf(DocumentValidationException.class);
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        new MockMultipartFile(
                                                "file",
                                                "script.exe",
                                                "application/octet-stream",
                                                new byte[] {1, 2, 3})))
                .isInstanceOf(DocumentValidationException.class);
    }

    @Test
    void detectsDocxByMagicBytesEvenWhenBrowserMimeIsGeneric() {
        byte[] zip = new byte[] {'P', 'K', 3, 4, 1, 2, 3};
        var result =
                validator.validate(
                        new MockMultipartFile("file", "law.docx", "application/octet-stream", zip));
        assertThat(result.mimeType())
                .isEqualTo(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
}
