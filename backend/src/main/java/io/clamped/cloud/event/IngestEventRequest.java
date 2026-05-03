package io.clamped.cloud.event;

import io.clamped.cloud.issue.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record IngestEventRequest(
        String fingerprint,
        @NotBlank String message,
        @NotNull EventSource source,
        @NotNull EventEnvironment environment,
        @NotNull Long projectId,
        Severity severity,
        List<String> tags,
        String exceptionClass,
        String stacktrace,
        String sourceFile,
        Integer sourceLine,
        String sourceMethod
) {}
