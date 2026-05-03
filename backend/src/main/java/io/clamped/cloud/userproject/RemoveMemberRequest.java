package io.clamped.cloud.userproject;

import jakarta.validation.constraints.NotNull;

public record RemoveMemberRequest(
        @NotNull Long projectId,
        @NotNull Long userId
        ) {}