package io.clamped.cloud.authentication;

import io.clamped.cloud.jwtconfig.JwtService;
import io.clamped.cloud.sessionconfig.SessionService;
import io.clamped.cloud.user.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
// Contains two endpoints that allow me to create/register a new account or authenticate an existing user
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final SessionService sessionService;

    // Adjust secure flag for localhost (false) vs HTTPS (true)
    // No longer used cuz its automatic with sessions
    // private static final boolean COOKIE_SECURE = false; set true in prod HTTPS (lombok ignores static fields)


    // Account creation
    // The service layer should not know anything about HTTP, sessions, headers, or web-specific APIs.
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request,              // 📩 Incoming request body (email, password, etc.)
            HttpServletRequest httpReq                        // 🌐 Used to get metadata like User-Agent, IP
    ) {
        // Create userPrincipal object and return the authenticated userPrincipal
        UserPrincipal userPrincipal = authenticationService.register(request);

        // Create session
        HttpSession session = httpReq.getSession(true); // true = create session if one doesn't exist

        // Store user ID and principal name index as session attributes
        session.setAttribute("userId", userPrincipal.getId());
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, userPrincipal.getId().toString());

        // Generate short-lived access token using principal
        String jwt = jwtService.generateAccessToken(userPrincipal);

        // 📦 Send access token (JWT) in response body — this is for frontend to use in Authorization headers
        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }

    // same http and calls as register in the controller (service logic is different)... we will override old access tokens in the front end
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @RequestBody AuthenticationRequest request,
            HttpServletRequest httpReq
    ) {
        // 1. Validate credentials and return authenticated UserPrincipal
        UserPrincipal userPrincipal = authenticationService.authenticate(request);

        // todo maybe invalidate existing sessions before creating them
        // todo I dont want the back button to cause login work arounds/hacks
        // todo I could just make it impossible to back track into login and only get there through log out idk
        // todo I might just not and let them expire
        // 2️. Create a new HttpSession (or reuse existing one) and implicitly store in cookie (no Response only Req)
        HttpSession newSession = httpReq.getSession(true); // ✅ Start fresh session for this login
        newSession.setAttribute("userId", userPrincipal.getId());
        newSession.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, userPrincipal.getId().toString());

        // 3. Store user ID (or any info) as session attribute
        //     Spring Session JDBC will persist this to the DB:
        //     - SPRING_SESSION: stores session ID, creation time, expiry, etc.
        //     - SPRING_SESSION_ATTRIBUTES: stores key-value pairs like ("userId", 123L)
        // newSession.setAttribute("userId", userPrincipal.getId());    // You could also store the principal here

        // 4. Generate short-lived access token using principal
        String jwt = jwtService.generateAccessToken(userPrincipal);

        // 5. Return access token in response body
        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }


    // Refresh access token if session (which has a lifetime of about 7 days to match refresh token functionality) is still valid
    // Access token would be dead by this point so no need to check if it is valid or not
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(HttpServletRequest httpReq) {
        HttpSession session = httpReq.getSession(false); // doesnt create new session

        // 🚫 If no session, unauthorized (no refresh token, no access)
        if (session == null || session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).build(); // catch this in front end and redirect to login page
        }

        Long userId = (Long) session.getAttribute("userId");
        UserPrincipal principal = authenticationService.getPrincipalById(userId);
        String newJwt = jwtService.generateAccessToken(principal);

        return ResponseEntity.ok(new AuthenticationResponse(newJwt));
    }

    // Logout: invalidate session
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpReq) {
        // Attempts to retrieve the current HTTP session, without creating a new one (false means "don't create if absent").
        // If there’s no session (user is already logged out), it returns null.
        sessionService.invalidateCurrentSession(httpReq);

        return ResponseEntity.noContent().build(); // Return 204: Kill Session, to be handled by front-end and re-directed to login page
    }

    // Logout: invalidate all existing sessions on all browsers
    // Uses the session cookie to identify the user (same pattern as /refresh)
    @PostMapping("/logoutAllSessions")
    public ResponseEntity<Void> logoutAllSessions(HttpServletRequest httpReq) {
        HttpSession session = httpReq.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = (Long) session.getAttribute("userId");
        UserPrincipal principal = authenticationService.getPrincipalById(userId);
        sessionService.invalidateAllSessions(userId.toString());

        return ResponseEntity.noContent().build();
    }
}
