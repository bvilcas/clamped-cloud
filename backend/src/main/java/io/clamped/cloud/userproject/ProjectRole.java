package io.clamped.cloud.userproject;

public enum ProjectRole {
    LEAD,    // full control: manage members, edit project, assign issues
    MEMBER   // contributor: report issues, self-assign as ASSIGNEE or VERIFIER on any issue
}