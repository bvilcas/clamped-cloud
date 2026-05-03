package io.clamped.cloud.userissue;

import jakarta.validation.constraints.NotNull;

public record UserProjectRelationshipRequest(
        @NotNull Long userId,
        @NotNull Long projectId
) {}
