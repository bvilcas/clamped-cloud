package io.clamped.cloud.issue;

import jakarta.validation.constraints.NotNull;

public record ChangeIssueStatusRequest(
        @NotNull Long projectId,
        @NotNull Long issueId,
        @NotNull IssueStatus newStatus
) {}
