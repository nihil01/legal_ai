package az.legalai.document.service;

public class DuplicateDocumentException extends RuntimeException {
    public DuplicateDocumentException() {
        super("Этот документ уже был загружен");
    }
}
