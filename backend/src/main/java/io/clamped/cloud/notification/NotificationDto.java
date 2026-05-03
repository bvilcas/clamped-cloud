package io.clamped.cloud.notification;

import java.time.Instant;

public record NotificationDto(
        Long id,
        NotificationType type,
        String message,
        Long relatedProjectId,
        Long relatedIssueId,
        boolean read,
        Instant createdAt
) {}
