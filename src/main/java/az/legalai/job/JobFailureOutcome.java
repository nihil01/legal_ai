package az.legalai.job;

public enum JobFailureOutcome {
    RETRY_SCHEDULED,
    TERMINAL_FAILED,
    LEASE_LOST
}
