package io.clamped.cloud.userissue;

import io.clamped.cloud.issue.IssueStatus;
import io.clamped.cloud.issue.IssueType;
import io.clamped.cloud.issue.Severity;

import java.time.Instant;

public record IssueWithProjectDto(
        Long id,
        String title,
        String description,
        IssueType type,
        Severity severity,
        IssueStatus status,
        Instant updatedAt,
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
        Long projectId,
        String projectName
) {}
