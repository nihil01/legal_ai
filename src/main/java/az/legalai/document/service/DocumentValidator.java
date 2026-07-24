package az.legalai.document.service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public final class DocumentValidator {
    private static final Map<String, String> TYPES =
            Map.of(
                    "docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "doc",
                    "application/msword",
                    "pdf",
                    "application/pdf",
                    "html",
                    "text/html",
                    "htm",
                    "text/html",
                    "txt",
                    "text/plain");
    private final long maxBytes;

    public DocumentValidator(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public ValidatedDocument validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new DocumentValidationException("Файл пуст");
        if (file.getSize() > maxBytes)
            throw new DocumentValidationException("Размер файла превышает лимит");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = extension(filename);
        if (!TYPES.containsKey(ext))
            throw new DocumentValidationException("Неподдерживаемый формат файла");
        try {
            byte[] bytes = file.getBytes();
            String mime = detect(ext, bytes);
            return new ValidatedDocument(filename, mime, bytes.length, sha256(bytes), bytes);
        } catch (IOException e) {
            throw new DocumentValidationException("Не удалось прочитать файл");
        }
    }

    private String detect(String ext, byte[] b) {
        if ("docx".equals(ext) && !starts(b, new byte[] {'P', 'K', 3, 4}))
            throw new DocumentValidationException("Содержимое файла не соответствует DOCX");
        if ("doc".equals(ext)
                && !starts(b, new byte[] {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}))
            throw new DocumentValidationException("Содержимое файла не соответствует DOC");
        if ("pdf".equals(ext) && !starts(b, "%PDF".getBytes()))
            throw new DocumentValidationException("Содержимое файла не соответствует PDF");
        return TYPES.get(ext);
    }

    private boolean starts(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (value[i] != prefix[i]) return false;
        return true;
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
