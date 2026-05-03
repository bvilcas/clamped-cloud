package io.clamped.cloud.user;

import io.clamped.cloud.authentication.AuthenticationResponse;
import io.clamped.cloud.backendconfig.ApiResponse;
import io.clamped.cloud.sessionconfig.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(path = "api/v1/users")

public class UserController {
    private final UserService userService;
    private final SessionService sessionService;

    public UserController(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserPrincipal> me(Authentication auth) {
        return ResponseEntity.ok((UserPrincipal) auth.getPrincipal());
    }

    @GetMapping("/profile")
    // @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')"), I dont technically need this
    // We use authentication to get the user object bc it would be a security risk otherwise
    // Gets Profile of User, User
    public ResponseEntity<ApiResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse(true, "Profile retrieved successfully",  userService.getProfile(authentication)));
    }

    // Update firstname and lastname (does not update jwt because we are not changing authentication info); Returns confirmation
    @PutMapping("/update")
    public ResponseEntity<ApiResponse> updateUserInfo(@RequestBody UpdateProfileRequest request,
                                                     Authentication authentication) {
        userService.updateUserInfo(request, authentication);
        return ResponseEntity.ok(new ApiResponse(true, "First or last name updated successfully"));
    }

    // Changes email and forces logout across all sessions bc it is used in authentication (userPrincipal which builds JWT)
    @PutMapping("/update/{email}")
    public ResponseEntity<AuthenticationResponse> changeEmail(
            @PathVariable String email,
            Authentication authentication
    ) {
        // changes the email in the user object
        userService.changeEmail(email, authentication);

        // ✅ Invalidate all sessions (including current session)
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
        sessionService.invalidateAllSessions(userId.toString());

        // This is only applies to the current session
        // 204 = no content, frontend should redirect to log-in where the user can get a new access token
        return ResponseEntity.noContent().build();
    }

    // Changes password and forces logout across all sessions
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        userService.changePassword(request, authentication);

        // Invalidate all sessions (including current session)
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
        sessionService.invalidateAllSessions(userId.toString());

        // This is only applies to the current session
        // 204 = no content, frontend should redirect to log-in where the user can get a new access token
        return ResponseEntity.noContent().build();
    }

    // Search users by email prefix — used for the add-member autocomplete in project pages
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchUsers(@RequestParam String email) {
        return ResponseEntity.ok(new ApiResponse(true, "Users found", userService.searchByEmail(email)));
    }

    @GetMapping("/email-notifications")
    public ResponseEntity<ApiResponse> getEmailNotifications(Authentication auth) {
        boolean enabled = userService.getEmailNotifications(auth);
        return ResponseEntity.ok(new ApiResponse(true, "Email notification setting retrieved", enabled));
    }

    @PutMapping("/email-notifications")
    public ResponseEntity<ApiResponse> setEmailNotifications(
            @RequestParam boolean enabled,
            Authentication auth
    ) {
        boolean result = userService.setEmailNotifications(enabled, auth);
        return ResponseEntity.ok(new ApiResponse(true,
                result ? "Email notifications enabled" : "Email notifications disabled", result));
    }
}
