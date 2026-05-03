package io.clamped.cloud.userproject;

import jakarta.validation.constraints.NotNull;

public record ChangeProjectRoleCommand(
        @NotNull Long projectId,
        @NotNull Long userId,
        @NotNull ProjectRole newRole
) {}