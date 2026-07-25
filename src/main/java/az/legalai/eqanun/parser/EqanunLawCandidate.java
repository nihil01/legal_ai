package az.legalai.eqanun.parser;

import java.time.LocalDate;

public record EqanunLawCandidate(
        String externalId,
        String externalVersionId,
        String title,
        String sourceUrl,
        LocalDate effectiveDate) {
    public EqanunLawCandidate {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("E-qanun external id must not be blank");
        }
        if (externalVersionId == null || externalVersionId.isBlank()) {
            throw new IllegalArgumentException("E-qanun version id must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("E-qanun title must not be blank");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("E-qanun source URL must not be blank");
        }
        if (effectiveDate == null) {
            throw new IllegalArgumentException("E-qanun effective date must not be null");
        }
    }
}
