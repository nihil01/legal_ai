package az.legalai.eqanun.service;

import java.time.Instant;

public record EqanunSyncReport(
        int discovered, int updated, int failed, Instant completedAt, String note) {
    public static EqanunSyncReport notConfigured() {
        return new EqanunSyncReport(
                0, 0, 0, Instant.now(), "E-qanun source adapter is not configured yet");
    }
}
