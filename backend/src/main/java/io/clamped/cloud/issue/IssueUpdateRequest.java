package io.clamped.cloud.issue;

import java.time.Instant;
import java.util.UUID;

public record IssueUpdateRequest(
        String title,
        String description,
        IssueType type,
        String cveId,
        String cweId,
        Severity severity,
        Instant dueAt,
        String repository,
        String commitHash,
        // Evidence reference — opaque pointer to local machine storage, never fetched by cloud
        UUID evidenceMachineId,
        UUID evidenceLocalId,
        String evidenceNote
) {}
