package io.clamped.cloud.userissue;

import jakarta.validation.constraints.NotNull;

public record IssueSelfAssignRevoke(
        @NotNull Long issueId,
        @NotNull Long projectId
) {}
