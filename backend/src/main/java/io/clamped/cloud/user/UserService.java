package io.clamped.cloud.user;

import io.clamped.cloud.authentication.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// Responsible for user account management
// Session killing should be instant (dealt with in the front; refresh tokens being revoked in the back end is all I can do for now)
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // true → cookie is only sent over HTTPS (production). EXTRA PRECAUTIONS FOR APP PROPERTIES WHICH ALREADY DEFINES THIS
    // false → cookie is sent over HTTP (local dev).
    // @Value("${app.auth.cookies.secure:true}")
    // private boolean cookieSecure;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // We don't need cookie info, but need security info for identifying the user
    // Return User cuz it has all profile info, we return userPrincipal in AuthServices because we needed to create authenticated shit (tokens)
    @Transactional(readOnly = true) // skips entity scan
    public User getProfile(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userRepository
                // lambda/java statement
                .findById(userPrincipal.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Your account was not found"));
    }

    // We don't need cookie info, but need security info for identifying that wants to change their first/last name
    public void updateUserInfo(UpdateProfileRequest request, Authentication authentication) {
        // check input existence
        if (request.getFirstName() == null && request.getLastName() == null) {
            throw new IllegalArgumentException("At least one field must be provided to update.");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElseThrow(()
                -> new UsernameNotFoundException("Your account was not found."));


        if (request.getFirstName() != null) {
            user.setFirstname(request.getFirstName()); // cant use builder cuz we are modifying field and not creating a new object
        }
        if (request.getLastName() != null) {
            user.setLastname(request.getLastName());
        }

        userRepository.save(user);
    }

    // Change email in user object for future and login identity (does not touch sessions... HTTP/controller responsibility)
    @Transactional
    public void changeEmail(String email, Authentication authentication) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("New email must be provided.");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Your account was not found."));


        if (email.equalsIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException("New email is the same as current email.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use.");
        }

        // ✅ Update email/username
        user.setUsername(email);
        // Invalidate access tokens from other sessions (which do not have instant re-login logic)
        user.setCredentialsChangedAt(Instant.now());

        userRepository.save(user);
    }

    // Change password in user object for future and login identity (does not touch sessions... HTTP/controller responsibility)
    @Transactional
    public void changePassword(ChangePasswordRequest request, Authentication authentication) {
        if (request.getOldPassword() == null || request.getNewPassword() == null) {
            throw new IllegalArgumentException("Both old and new passwords are required.");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different.");
        }

        // ✅ Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Invalidate access tokens from other sessions (which do not have instant re-login logic)
        user.setCredentialsChangedAt(Instant.now());

        userRepository.save(user);
    }

    @Transactional
    public boolean setEmailNotifications(boolean enabled, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEmailNotificationsEnabled(enabled);
        userRepository.save(user);
        return enabled;
    }

    @Transactional(readOnly = true)
    public boolean getEmailNotifications(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return userRepository.findById(principal.getId())
                .map(User::isEmailNotificationsEnabled)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<UserSearchResult> searchByEmail(String query) {
        if (query == null || query.isBlank() || query.length() < 2) return List.of();
        return userRepository.findTop10ByEmailContainingIgnoreCase(query)
                .stream()
                .map(u -> new UserSearchResult(u.getId(), u.getEmail(), u.getFirstname(), u.getLastname()))
                .toList();
    }
}
