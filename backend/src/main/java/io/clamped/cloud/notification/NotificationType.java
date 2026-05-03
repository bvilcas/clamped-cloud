package io.clamped.cloud.notification;

public enum NotificationType {
    ISSUE_ASSIGNED,        // You were assigned to an issue by a lead
    ISSUE_UNASSIGNED,      // You were unassigned from an issue
    ISSUE_REPORTED,        // A new issue was reported in your project
    ISSUE_STATUS_CHANGED,  // An issue you are on changed status
    ISSUE_SELF_ASSIGNED,   // Someone self-assigned to an issue (for leads)
    ISSUE_SELF_REVOKED,    // Someone self-revoked from an issue (for leads)
    PROJECT_ADDED,         // You were added to a project
    PROJECT_REMOVED        // You were removed from a project
}
