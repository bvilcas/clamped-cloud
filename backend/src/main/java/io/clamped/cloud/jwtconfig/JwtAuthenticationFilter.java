package io.clamped.cloud.jwtconfig;

import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor // lombak; creates constructor using any final field declared below (does dependency injection for us)
// Validate JWT and AuthenticationFilter
// This filter runs once per request and checks if a JWT token is present and valid. If it is, it sets up authentication in the SecurityContext.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    // we want our own implementation
    private final UserDetailsService userDetailsService;

    private final UserRepository  userRepository;
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization"); // header that contains JWT/bearer token
        final String jwt;
        final String userEmail;
        // check if it is a jwt token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // passes filter without a seal of authorization (will be destroyed in SecurityConfig)
            return;
        }
        // extracts the token and saves it into a string constant
        jwt = authHeader.substring(7);

        // Ignore refresh tokens here; only access tokens should authenticate requests
        if (!jwtService.isAccessToken(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            userEmail = jwtService.extractUsername(jwt);

            // check if user exists and when credentials were last checked
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                Instant issuedAt = jwtService.extractIssuedAt(jwt).toInstant();
                Instant credsChangedAt = user.getCredentialsChangedAt();

                if (credsChangedAt != null && issuedAt.isBefore(credsChangedAt)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                // load user if passes filter tests
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // 🔴 Any bad JWT → reject gracefully
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
