package az.legalai.document.service;

public class DuplicateDocumentException extends RuntimeException {
    public DuplicateDocumentException() {
        super("Bu sənəd artıq yüklənib");
    }
}
