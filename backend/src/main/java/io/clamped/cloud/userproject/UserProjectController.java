package io.clamped.cloud.userproject;
import io.clamped.cloud.backendconfig.ApiResponse;
import io.clamped.cloud.project.Project;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "api/v1/userprojects")
public class UserProjectController {

    /// ADDING, REMOVING OTHER LEADS, AND CHANGING OTHER ROLES INTO LEADS ASK FOR CONFIRMATION
    /// CODES: PROMOTING_MEMBER_LEAD, ADDING_MEMBER_LEAD, REMOVING_MEMBER_LEAD

    /// DELETING YOURSELF (LAST LEAD) WILL GIVE WARNING AND FORCE YOU TO PROMOTE SOMEONE (CHANGEUSERROLE)
    /// CODES: "SELF_REMOVE_LAST_MEMBER" (CASCADE DELETE UPON VERIFICATION), "SELF_REMOVE_LAST_LEAD"
    private final UserProjectService userProjectService;

    public UserProjectController(UserProjectService userProjectService) {
        this.userProjectService = userProjectService;
    }

    ///  PROJECT MANAGEMENT

    @GetMapping("/all")
    public ResponseEntity<?> getMyProjects(Authentication authentication) {
        try {
            var data = userProjectService.getMyProjects(authentication);
            System.out.println("✅ Retrieved projects: " + data.size());
            return ResponseEntity.ok(new ApiResponse(true, "Projects retrieved successfully", data));
        } catch (Exception e) {
            e.printStackTrace();  // <--- THIS prints the true 500 cause
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // I can also create a sort method that filters by created at in the front end (sort order)
    // This would return an ordered list that would be pasted in the front end
    // getMyProjectsSortedByCreatedAt

    // Get MY project by id
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse> getMyProjectById(@PathVariable Long projectId, Authentication authentication) {
        ProjectSummaryDto project = userProjectService.getMyProjectById(authentication, projectId);
        return ResponseEntity.ok(new ApiResponse(true, "Project retrieved", project));
    }

    // Search by name
    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse> getMyProjectsByName(@PathVariable String name, Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse(true, "Project found", userProjectService.getMyProjectByName(authentication, name)));
    }

    // THIS CAN PREVENT LEADS PROMOTING OTHER LEADS WITHOUT NOTIFICATION (CAUGHT IN FRONTEND)
    // Lead method only (to apply on users and themselves)
    @PreAuthorize("@projectSecurity.hasProjectRole(authentication, #request.projectId(), 'LEAD')")
    @PostMapping("/change")
    public ResponseEntity<ApiResponse> changeUserRoleInProject(@RequestBody ChangeProjectRoleCommand request) {
        RoleCheckerResponse response = userProjectService.changeUserRoleInProject(request);
        return ResponseEntity.ok(new ApiResponse(true, "User project role updated successfully", response));
    }

    ///  TEAM/USER MANAGEMENT
    @PreAuthorize("@projectSecurity.isProjectMember(authentication, #projectId)")
    @GetMapping("/members/{projectId}")
    public ResponseEntity<ApiResponse> getProjectMembers(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                new ApiResponse(true, "Members retrieved", userProjectService.getProjectMembers(projectId))
        );
    }

    @PreAuthorize("@projectSecurity.hasProjectRole(authentication, #request.projectId(), 'LEAD')")
    @PostMapping("/members/add")
    public ResponseEntity<ApiResponse> addMemberToProject(@RequestBody AddMemberRequest request, Authentication authentication) {
        RoleCheckerResponse response = userProjectService.addMemberToProject(request);
        return ResponseEntity.ok(new ApiResponse(true, "Member added successfully", response));
    }

    // Verify that we can remove the member (called before RemoveMemberFromProject)
    @PreAuthorize("@projectSecurity.hasProjectRole(authentication, #request.projectId(), 'LEAD')")
    @PostMapping("/members/remove/validate")
    public ResponseEntity<ApiResponse> validateRemoveMember(@RequestBody RemoveMemberRequest request) {
        return ResponseEntity.ok(new ApiResponse(true, "Validation successful", userProjectService.validateRemoveMember(request)));
    }

    // If removing a lead, there must be confirmation before going through.
    @PreAuthorize("@projectSecurity.hasProjectRole(authentication, #request.projectId(), 'LEAD')")
    @DeleteMapping("/members/remove")
    public ResponseEntity<ApiResponse> removeMemberFromProject(@RequestBody RemoveMemberRequest request) {
        userProjectService.removeMemberFromProject(request);
        return ResponseEntity.ok(new ApiResponse(true, "Member removed successfully"));
    }

    // Verify that we can self remove (gives deletion code)
    @PreAuthorize("@projectSecurity.isProjectMember(authentication, #projectId)")
    @PostMapping("/self-remove/validate/{projectId}")
    public ResponseEntity<ApiResponse> validateSelfRemove(@PathVariable Long projectId, Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse(true, "Validation successful", userProjectService.validateSelfRemove(projectId, authentication)));
    }

    /// THIS METHOD JUST DELETES, it is up to the frontend to validate before deleting/calling this method
    @PreAuthorize("@projectSecurity.isProjectMember(authentication, #projectId)")
    @DeleteMapping("/self-remove/{projectId}")
    public ResponseEntity<ApiResponse> selfRemoveFromProject(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        userProjectService.selfRemoveFromProject(projectId, authentication);
        return ResponseEntity.ok(new ApiResponse(true, "You have removed yourself successfully"));
    }

    ///  MERGE LOGIC USING NOTNULL AND NOTBLANKS (FIXED FOR UPDATE METHODS)

    ///  UPPERCASE STUFF IS HANDLED IN FRONTEND, BACKEND SIMPLY CAStS ENUMS WITHIN THE RECORDS
}
