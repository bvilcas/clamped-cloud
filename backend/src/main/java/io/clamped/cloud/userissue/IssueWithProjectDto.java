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
        String cveId,
        String cweId,
        Severity severity,
        IssueStatus status,
        Instant updatedAt,
        Instant reportedAt,
        Instant dueAt,
        Instant patchedAt,
        Instant verifiedAt,
        String repository,
        String commitHash,
        Long projectId,
        String projectName
) {}
