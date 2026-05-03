package io.clamped.cloud.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectCreationRequest(
    @NotBlank String name,
    String description
){}
