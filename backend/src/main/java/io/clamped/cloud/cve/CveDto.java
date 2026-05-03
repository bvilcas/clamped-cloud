package io.clamped.cloud.cve;

public record CveDto(
        String cveId,
        String description,
        Double cvssScore,
        String cvssVector,
        String severity,
        String cweId,
        String published,
        String lastModified
) {}
