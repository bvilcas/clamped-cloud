package io.clamped.cloud.admin;

import io.clamped.cloud.authentication.AuthenticationService;
import io.clamped.cloud.authentication.RegisterRequest;
import io.clamped.cloud.backendconfig.ApiResponse;
import io.clamped.cloud.issue.Issue;
import io.clamped.cloud.issue.Severity;
import io.clamped.cloud.project.Project;
import io.clamped.cloud.project.ProjectService;
import io.clamped.cloud.sessionconfig.SessionService;
import io.clamped.cloud.user.Role;
import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping(path = "api/v1/admin")
public class AdminController {
    private final AdminService adminService;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final ProjectService projectService;

    public AdminController(AdminService adminService, AuthenticationService authenticationService,
                           UserRepository userRepository, SessionService sessionService,
                           ProjectService projectService) {
        this.adminService = adminService;
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.projectService = projectService;
    }

    // USER FUNCTIONS

    // 🔐 ADMIN: View all users; Returns List<User>
    @GetMapping(path = "/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getUsers() {
        return ResponseEntity.ok(new ApiResponse(true, "Users retrieved successfully", adminService.getUsers()));
    }

    // 🔐 ADMIN: Find specific user by email; Returns User
    @GetMapping(path = "/users/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getUserByEmail(@PathVariable("email") String email) {
        return ResponseEntity.ok(new ApiResponse(true, "User with email " + email + " retrieved successfully", adminService.getUserByEmail(email)));
    }

    @PostMapping(path = "/users/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> addUser(@RequestBody RegisterRequest request,
                                               @RequestParam(defaultValue = "USER") Role role) {
        User adminCreatedUser = authenticationService.registerAsAdmin(request, role);
        return ResponseEntity.ok(new ApiResponse(true, "User added successfully", adminCreatedUser));
    }

    @PutMapping(path = "/users/update/info/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateUserFirstOrLastName(
            @PathVariable("userId") Long userId,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        adminService.updateUserFirstOrLastName(userId, firstName, lastName);
        return ResponseEntity.ok(new ApiResponse(true, "First or Last name changed successfully"));
    }

    @PutMapping(path = "/users/update/credentials/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateUserEmailOrPassword(
            @PathVariable("userId") Long userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String password) {
        User user = adminService.updateUserEmailOrPassword(userId, email, password);
        sessionService.invalidateAllSessions(user.getId().toString());
        user.setCredentialsChangedAt(Instant.now());
        return ResponseEntity.ok(new ApiResponse(true, "Email or password changed successfully"));
    }

    @Transactional
    @DeleteMapping(path = "/users/delete/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        sessionService.invalidateAllSessions(user.getId().toString());
        user.setCredentialsChangedAt(Instant.now());
        userRepository.deleteById(userId);
    }

    // =======================================Issue Functions===========================================
    @GetMapping(path = "/issues/all")
    public ResponseEntity<ApiResponse> getIssues() {
        return ResponseEntity.ok(new ApiResponse(true, "Issues retrieved successfully", adminService.getIssues()));
    }

    @GetMapping("/issues/{issueId}")
    public ResponseEntity<ApiResponse> getIssueById(@PathVariable Long issueId) {
        Issue issue = adminService.getIssueById(issueId);
        return ResponseEntity.ok(new ApiResponse(true, "Issue retrieved", issue));
    }

    @GetMapping("/issues/title/{title}")
    public ResponseEntity<ApiResponse> getIssueByTitle(@PathVariable String title) {
        Issue issue = adminService.getIssueByTitle(title);
        return ResponseEntity.ok(new ApiResponse(true, "Issue found", issue));
    }

    @GetMapping("/issues/severity/{severity}")
    public ResponseEntity<ApiResponse> getIssuesBySeverity(@PathVariable Severity severity) {
        return ResponseEntity.ok(new ApiResponse(true, "Filtered by severity", adminService.getIssuesBySeverity(severity)));
    }

    @GetMapping("/issues/overdue")
    public ResponseEntity<ApiResponse> getOverdueIssues() {
        return ResponseEntity.ok(new ApiResponse(true, "Overdue issues", adminService.getOverdueIssues()));
    }

    // ======================================== Project Functions ==================================================
    @GetMapping
    public ResponseEntity<ApiResponse> getAllProjects() {
        List<Project> projects = adminService.getAllProjects();
        return ResponseEntity.ok(new ApiResponse(true, "Projects retrieved", projects));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getProjectById(@PathVariable Long id) {
        Project project = adminService.getProjectById(id);
        return ResponseEntity.ok(new ApiResponse(true, "Project retrieved", project));
    }
}
