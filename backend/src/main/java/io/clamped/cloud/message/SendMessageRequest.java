package io.clamped.cloud.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(
        @NotNull Long projectId,
        @NotBlank String content
) {}
