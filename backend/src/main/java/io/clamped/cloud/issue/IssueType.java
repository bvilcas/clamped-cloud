package io.clamped.cloud.issue;

public enum IssueType {
    SECURITY,       // Auth, data exposure, dependency risk
    RELIABILITY,    // Service crashes, timeouts, data loss
    PERFORMANCE,    // Regressions, SLA violations
    UX,             // User-facing defects, accessibility
    OTHER           // Default / uncategorised
}
