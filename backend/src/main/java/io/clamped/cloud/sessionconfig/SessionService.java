package io.clamped.cloud.sessionconfig;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final JdbcTemplate jdbcTemplate;

    // Invalidate the current session
    public void invalidateCurrentSession(HttpServletRequest httpReq) {
        // Attempts to retrieve the current HTTP session, without creating a new one (false means "don't create if absent").
        // If there’s no session (user is already logged out), it returns null.
        if (httpReq.getCookies() != null) {
            Arrays.stream(httpReq.getCookies())
                    .forEach(c -> System.out.println("🍪 Cookie: " + c.getName() + " = " + c.getValue()));
        } else {
            System.out.println("🚫 No cookies received");
        }

        HttpSession session = httpReq.getSession(false);
        if (session != null) {
            System.out.println("✅ Invalidating session: " + session.getId());
            session.invalidate();
        } else {
            System.out.println("⚠️ No session found for request");
        }
    }

    // Invalidate all sessions for a given user by principal name
    // Uses the PRINCIPAL_NAME column on SPRING_SESSION, which is populated
    // when we set FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME as a session attribute
    public void invalidateAllSessions(String principalName) {
        jdbcTemplate.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?", principalName);
    }
}

