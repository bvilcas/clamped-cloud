package io.clamped.cloud.event;

import io.clamped.cloud.issue.Severity;

import java.time.Instant;

public record EventGroupDto(
        Long id,
        String fingerprint,
        String message,
        EventSource source,
        EventEnvironment environment,
        Severity severity,
        Long count,
        Instant firstSeen,
        Instant lastSeen,
        boolean muted,
        String tags,
        String exceptionClass,
        String stacktrace,
        String sourceFile,
        Integer sourceLine,
        String sourceMethod,
        Long linkedIssueId,
        Long projectId
) {}
