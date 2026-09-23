package io.clamped.cloud.userissue;

import io.clamped.cloud.issue.IssueStatus;
import io.clamped.cloud.issue.Severity;

import java.time.Instant;
import java.util.List;

public record IssueAssignmentDto(
        Long id,
        String title,
        String description,
        Severity severity,
        IssueStatus status,
        Instant reportedAt,
        Instant dueAt,
        Instant patchedAt,
        Instant verifiedAt,
        String repository,
        String commitHash,
        String eventApp,
        String eventHost,
        String eventType,
        String eventExtra,
        List<AssignmentEntry> assignments
) {}
