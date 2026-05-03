package io.clamped.cloud.calendar;

import io.clamped.cloud.issue.IssueStatus;
import io.clamped.cloud.issue.Severity;

import java.time.Instant;

public record CalendarVulnDto(
        Long id,
        String title,
        Severity severity,
        IssueStatus status,
        Instant dueAt,
        Long projectId,
        String projectName
) {}
