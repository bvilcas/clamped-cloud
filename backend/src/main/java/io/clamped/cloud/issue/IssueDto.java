package io.clamped.cloud.issue;

import java.time.Instant;

public record IssueDto(
        Long id,
        String title,
        String description,
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
        Long projectId
) {
    public static IssueDto from(Issue issue) {
        return new IssueDto(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getSeverity(),
                issue.getStatus(),
                issue.getUpdatedAt(),
                issue.getReportedAt(),
                issue.getDueAt(),
                issue.getPatchedAt(),
                issue.getVerifiedAt(),
                issue.getRepository(),
                issue.getCommitHash(),
                issue.getEventApp(),
                issue.getEventHost(),
                issue.getEventType(),
                issue.getEventExtra(),
                issue.getProject().getId()
        );
    }
}
