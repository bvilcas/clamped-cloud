package io.clamped.cloud.userissue;

import io.clamped.cloud.backendconfig.ApiResponse;
import io.clamped.cloud.issue.Issue;
import io.clamped.cloud.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/userissues")
public class UserIssueController {

    private final UserIssueService userIssueService;

    public UserIssueController(UserIssueService userIssueService) {
        this.userIssueService = userIssueService;
    }

    @PreAuthorize("@projectSecurity.isProjectMember(authentication, #selfAssign.projectId())")
    @PostMapping("/self-assign")
    public ResponseEntity<ApiResponse> selfAssignToIssue(@RequestBody IssueSelfAssignRevoke selfAssign, Authentication auth) {
        userIssueService.selfAssignToIssue(selfAssign, auth);
        return ResponseEntity.ok(new ApiResponse(true, "You have assigned yourself to this issue"));
    }

    @PreAuthorize("@projectSecurity.isProjectMember(authentication, #selfRevoke.projectId())")
    @DeleteMapping("/self-revoke")
    public ResponseEntity<ApiResponse> selfRevokeFromIssue(@RequestBody IssueSelfAssignRevoke selfRevoke, Authentication auth) {
        userIssueService.selfRevokeFromIssue(selfRevoke, auth);
        return ResponseEntity.ok(new ApiResponse(true, "You have withdrawn yourself from this issue"));
    }

    @PreAuthorize("@projectSecurity.hasProjectRole(authentication, #action.projectId(), 'LEAD')")
    @PostMapping("/assign")
    public ResponseEntity<ApiResponse> assignUserToIssue(@RequestBody IssueAssignAction action) {
        User user = userIssueService.assignUserToIssue(action);
        return ResponseEntity.ok(new ApiResponse(true,
                "User " + user.getFirstname() + " was assigned as " + action.role() + " successfully"));
    }

    @PreAuthorize("@projectSecurity.hasProjectRole(authentication, #action.projectId(), 'LEAD')")
    @DeleteMapping("/unassign")
    public ResponseEntity<ApiResponse> revokeUserFromIssue(@RequestBody IssueRevokeAction action) {
        User user = userIssueService.revokeUserFromIssue(action);
        return ResponseEntity.ok(new ApiResponse(true, "User " + user.getFirstname() + " was unassigned successfully"));
    }

    @GetMapping("/reported")
    public ResponseEntity<ApiResponse> getAllReportedByMe(Authentication authentication) {
        List<IssueWithProjectDto> reported = userIssueService.getReportedByMe(authentication);
        return ResponseEntity.ok(new ApiResponse(true, reported.isEmpty() ? "No issues found" : "Issues returned", reported));
    }

    @PreAuthorize("@projectSecurity.isProjectMember(authentication, #projectId)")
    @GetMapping("/reported/{projectId}")
    public ResponseEntity<ApiResponse> getReportedByMeInProject(@PathVariable Long projectId, Authentication authentication) {
        List<Issue> reported = userIssueService.getReportedByMeInProject(projectId, authentication);
        return ResponseEntity.ok(new ApiResponse(true, reported.isEmpty() ? "No issues found" : "Issues returned", reported));
    }

    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse> getAssignedToMe(Authentication authentication) {
        List<IssueWithProjectDto> assigned = userIssueService.getAssignedToMe(authentication);
        return ResponseEntity.ok(new ApiResponse(true, assigned.isEmpty() ? "No issues found" : "Issues returned", assigned));
    }

    @PreAuthorize("@projectSecurity.isProjectMember(authentication, #projectId)")
    @GetMapping("/assigned/{projectId}")
    public ResponseEntity<ApiResponse> getAssignedToMeInProject(@PathVariable Long projectId, Authentication authentication) {
        List<Issue> assigned = userIssueService.getAssignedToMeInProject(projectId, authentication);
        return ResponseEntity.ok(new ApiResponse(true, assigned.isEmpty() ? "No issues found" : "Issues returned", assigned));
    }

    @GetMapping("/verified")
    public ResponseEntity<ApiResponse> getVerifiedByMe(Authentication authentication) {
        List<IssueWithProjectDto> verified = userIssueService.getVerifiedByMe(authentication);
        return ResponseEntity.ok(new ApiResponse(true, verified.isEmpty() ? "No issues found" : "Issues returned", verified));
    }

    @PreAuthorize("@projectSecurity.isProjectMember(authentication, #projectId)")
    @GetMapping("/verified/{projectId}")
    public ResponseEntity<ApiResponse> getVerifiedByMeInProject(@PathVariable Long projectId, Authentication authentication) {
        List<Issue> verified = userIssueService.getVerifiedByMeInProject(projectId, authentication);
        return ResponseEntity.ok(new ApiResponse(true, verified.isEmpty() ? "No issues found" : "Issues returned", verified));
    }

    @PreAuthorize("@projectSecurity.isProjectMember(authentication, #projectId)")
    @GetMapping("/project-assignments/{projectId}")
    public ResponseEntity<ApiResponse> getProjectAssignments(@PathVariable Long projectId) {
        List<IssueAssignmentDto> result = userIssueService.getProjectAssignments(projectId);
        return ResponseEntity.ok(new ApiResponse(true, "Project assignments returned", result));
    }

    @PreAuthorize("@projectSecurity.hasProjectRole(authentication, #request.projectId(), 'LEAD')")
    @GetMapping("/reported-by-user")
    public ResponseEntity<ApiResponse> getReportedByUserInProject(@RequestBody UserProjectRelationshipRequest request) {
        List<Issue> reported = userIssueService.getReportedByUserInProject(request);
        return ResponseEntity.ok(new ApiResponse(true, reported.isEmpty() ? "No issues found" : "Issues returned", reported));
    }

    @PreAuthorize("@projectSecurity.hasProjectRole(authentication, #request.projectId(), 'LEAD')")
    @GetMapping("/assigned-to-user")
    public ResponseEntity<ApiResponse> getAssignedToUserInProject(@RequestBody UserProjectRelationshipRequest request) {
        List<Issue> assigned = userIssueService.getAssignedToUserInProject(request);
        return ResponseEntity.ok(new ApiResponse(true, assigned.isEmpty() ? "No issues found" : "Issues returned", assigned));
    }

    @PreAuthorize("@projectSecurity.hasProjectRole(authentication, #request.projectId(), 'LEAD')")
    @GetMapping("/verified-by-user")
    public ResponseEntity<ApiResponse> getVerifiedByUserInProject(@RequestBody UserProjectRelationshipRequest request) {
        List<Issue> verified = userIssueService.getVerifiedByUserInProject(request);
        return ResponseEntity.ok(new ApiResponse(true, verified.isEmpty() ? "No issues found" : "Issues returned", verified));
    }
}
