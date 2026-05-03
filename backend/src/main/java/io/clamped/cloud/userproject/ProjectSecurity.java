package io.clamped.cloud.userproject;

import io.clamped.cloud.user.Role;
import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserPrincipal;
import io.clamped.cloud.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

// Makes sure a user has access to a specific project and that they cant delete the whole thing
@Component("projectSecurity")
public class ProjectSecurity {
    private final UserProjectRepository userProjectRepository;

    public ProjectSecurity(UserProjectRepository userProjectRepository) {
        this.userProjectRepository = userProjectRepository;
    }

    // Only users with the required role (or admins) can perform the action
    public boolean hasProjectRole(Authentication auth, Long projectId, String requiredRole) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getId();

        // ✅ Always trust the authenticated principal’s role first
        if (principal.getRole() == Role.ADMIN)
            return true;

        // ✅ Check user’s membership in the project
        return userProjectRepository.findByUserIdAndProjectId(userId, projectId)
                .map(up -> up.getRole().name().equalsIgnoreCase(requiredRole))
                .orElse(false);
    }

    // ✅ Check if user is a project member at all
    /**
     * ✅ Checks if the user is simply a member of the project (regardless of their specific role).
     * Admin always passes this check.
     */
    public boolean isProjectMember(Authentication auth, Long projectId) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getId();

        if (principal.getRole() == Role.ADMIN)
            return true;

        return userProjectRepository.findByUserIdAndProjectId(userId, projectId).isPresent();
    }

    // ✅ Get project-specific role for current user
    public ProjectRole getProjectRole(Authentication auth, Long projectId) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getId();

        if (principal.getRole() == Role.ADMIN)
            return ProjectRole.LEAD;

        UserProject membership = userProjectRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this project"));

        return membership.getRole();
    }
}
