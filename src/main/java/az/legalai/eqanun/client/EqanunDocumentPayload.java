package az.legalai.eqanun.client;

public record EqanunDocumentPayload(byte[] bytes, String filename, String mimeType) {
    public EqanunDocumentPayload {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("E-qanun document body must not be empty");
        }
        bytes = bytes.clone();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("E-qanun document filename must not be blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("E-qanun document MIME type must not be blank");
        }
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
