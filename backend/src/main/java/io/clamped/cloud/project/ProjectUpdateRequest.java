package io.clamped.cloud.project;

public record ProjectUpdateRequest(
        String name,
        String description
) {
}
