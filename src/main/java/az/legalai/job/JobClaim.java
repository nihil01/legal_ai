package az.legalai.job;

import java.util.UUID;

public record JobClaim(UUID id, UUID documentId, int attempts, String worker, UUID lockToken) {}
