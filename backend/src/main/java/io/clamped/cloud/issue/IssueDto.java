package io.clamped.cloud.issue;

import java.time.Instant;

public record IssueDto(
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
        Long projectId
) {
    public static IssueDto from(Issue issue) {
        return new IssueDto(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getType(),
                issue.getCveId(),
                issue.getCweId(),
                issue.getSeverity(),
                issue.getStatus(),
                issue.getUpdatedAt(),
                issue.getReportedAt(),
                issue.getDueAt(),
                issue.getPatchedAt(),
                issue.getVerifiedAt(),
                issue.getRepository(),
                issue.getCommitHash(),
                issue.getProject().getId()
        );
    }
}
