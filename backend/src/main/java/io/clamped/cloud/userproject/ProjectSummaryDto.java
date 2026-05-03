package io.clamped.cloud.userproject;

import java.time.Instant;

public record ProjectSummaryDto(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        String myRole
) {}
