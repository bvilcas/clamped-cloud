package io.clamped.cloud.userissue;

import jakarta.validation.constraints.NotNull;

public record IssueRevokeAction(
        @NotNull Long userId,
        @NotNull Long issueId,
        @NotNull Long projectId
) {}
