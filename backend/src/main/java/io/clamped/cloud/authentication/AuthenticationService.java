package io.clamped.cloud.authentication;

import io.clamped.cloud.user.UserPrincipal;
import io.clamped.cloud.user.Role;
import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j // lombok logger
@Service
@RequiredArgsConstructor
// Implementation of register and authentication methods in controller
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    // Registers a new user in db and returns authenticated version for jwt creation in controller
    @Transactional
    public UserPrincipal register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Application-level pre-check
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = User.builder() // lombok import
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // basically a sophisticated hash for passwords
                .role(Role.USER) // todo change this for registering admins, maybe you need to add a parameter idk?
                .build();
        userRepository.save(user); // saves the user into the database

        // Step 3: Build and return UserPrincipal
        return UserPrincipal.create(user);
    }

    // Verifies credentials and returns userPrincipal (for Spring Security)
    public UserPrincipal authenticate(AuthenticationRequest request) {
        // Step 1: Authenticate credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Step 2: Look up user by email (not ID)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Step 3: Build and return UserPrincipal
        return UserPrincipal.create(user);
    }

    // Get UserPrincipal from user object's id (good for the refresh implementation in controller)
    @Transactional
    public UserPrincipal getPrincipalById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return UserPrincipal.create(user);
    }

    // ============================================ ADMIN METHODS =====================================================
    @Transactional
    public User registerAsAdmin(RegisterRequest request, Role role) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user); // return User (not UserPrincipal)
        return user;
    }
}
