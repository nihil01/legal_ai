package az.legalai.eqanun.service;

import java.time.Instant;

public record EqanunSyncReport(
        int discovered, int updated, int unchanged, int failed, Instant completedAt, String note) {
    public EqanunSyncReport {
        if (discovered < 0 || updated < 0 || unchanged < 0 || failed < 0) {
            throw new IllegalArgumentException("E-qanun sync counters must not be negative");
        }
    }

    public static EqanunSyncReport notConfigured() {
        return new EqanunSyncReport(
                0, 0, 0, 0, Instant.now(), "E-qanun source adapter is not configured yet");
    }
}
