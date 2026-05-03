package io.clamped.cloud.userproject;

public enum ProjectRole {
    LEAD,   // full control: manage members, edit project, assign vulns
    PROGRAMMER,  // normal contributor: report vulns, self-assign
    TESTER   // verify patched stuff
}