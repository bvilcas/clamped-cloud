package io.clamped.cloud.userproject;

import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @NotNull Long projectId,
        @NotNull Long userId,
        @NotNull ProjectRole role // e.g. "USER", "LEAD"
) {}
