package az.legalai.eqanun.parser;

import java.time.Instant;

public record EqanunLawCandidate(
        String externalId, String title, String sourceUrl, Instant sourceModifiedAt) {
    public EqanunLawCandidate {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("E-qanun external id must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("E-qanun title must not be blank");
        }
    }
}
