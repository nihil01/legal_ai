package az.legalparser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class DocumentFormatDetector {
    private static final byte[] ZIP_MAGIC = {'P', 'K', 3, 4};
    private static final byte[] OLE_MAGIC = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    public DocumentFormat detect(Path file) throws IOException {
        byte[] header = new byte[8];
        int bytesRead;
        try (InputStream input = Files.newInputStream(file)) {
            bytesRead = input.read(header);
        }

        if (bytesRead >= 4 && Arrays.equals(Arrays.copyOf(header, 4), ZIP_MAGIC)) {
            return DocumentFormat.DOCX;
        }
        if (bytesRead >= 8 && Arrays.equals(header, OLE_MAGIC)) {
            return DocumentFormat.LEGACY_DOC;
        }
        return DocumentFormat.UNKNOWN;
    }
}
