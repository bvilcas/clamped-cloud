package io.clamped.cloud.userissue;

import jakarta.validation.constraints.NotNull;

public record IssueAssignAction(
        @NotNull Long userId,
        @NotNull Long issueId,
        @NotNull Long projectId,
        @NotNull RoleInIssue role
) {}
