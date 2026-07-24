package az.legalai.document.service;

public record ValidatedDocument(
        String originalFilename, String mimeType, long size, String checksum, byte[] bytes) {}
