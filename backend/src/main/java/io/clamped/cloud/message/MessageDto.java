package io.clamped.cloud.message;

import java.time.Instant;

public record MessageDto(
        Long id,
        String content,
        Long senderId,
        String senderFirstname,
        String senderLastname,
        Long projectId,
        Instant sentAt
) {}
