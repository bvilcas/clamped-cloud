package io.clamped.cloud.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record IssueReportRequest(
        @NotBlank String title,
        String description,
        @NotNull Severity severity,
        @NotNull IssueStatus status,
        @NotNull Instant dueAt,
        String repository,
        String commitHash,
        // Evidence reference — opaque pointer to local machine storage, never fetched by cloud
        UUID evidenceMachineId,
        UUID evidenceLocalId,
        String evidenceNote
) {}
