package io.clamped.cloud.issue;

public enum IssueType {
    SECURITY,       // CWE/CVE applicable
    RELIABILITY,    // Service crashes, timeouts, data loss
    PERFORMANCE,    // Regressions, SLA violations
    UX,             // User-facing defects, accessibility
    OTHER           // Default / uncategorised
}
