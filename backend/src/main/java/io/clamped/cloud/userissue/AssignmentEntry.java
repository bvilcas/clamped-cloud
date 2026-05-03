package io.clamped.cloud.userissue;

import java.time.Instant;

public record AssignmentEntry(
        Long userId,
        String firstname,
        String lastname,
        RoleInIssue role,
        Instant assignedAt,
        boolean selfAssigned
) {}
