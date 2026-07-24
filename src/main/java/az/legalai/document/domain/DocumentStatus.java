package az.legalai.document.domain;

public enum DocumentStatus {
    UPLOADED,
    PARSING,
    CLEANING,
    STRUCTURE_PARSING,
    CHUNKING,
    EMBEDDING,
    COMPLETED,
    FAILED
}
