package io.clamped.cloud.userproject;

public record ProjectMemberResponse(
        Long id,
        String firstname,
        String lastname,
        ProjectRole projectRole
) {}
