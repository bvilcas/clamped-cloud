package io.clamped.cloud.userissue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserIssueRepository extends JpaRepository<UserIssue, Long> {

    // --- Active (current) assignments ---

    List<UserIssue> findByUserIdAndRevokedAtIsNull(Long userId);

    List<UserIssue> findByUserIdAndRoleAndRevokedAtIsNull(Long userId, RoleInIssue role);

    Optional<UserIssue> findByUserIdAndIssueIdAndRevokedAtIsNull(Long userId, Long issueId);

    boolean existsByUserIdAndIssueIdAndRevokedAtIsNull(Long userId, Long issueId);

    boolean existsByIssueIdAndRoleAndRevokedAtIsNull(Long issueId, RoleInIssue role);

    // --- Full history (active + revoked) ---
    // Used to check whether a user has EVER held a given role on an issue, even after
    // revoking - this is what prevents self-certification (see UserIssue.revokedAt).
    boolean existsByUserIdAndIssueIdAndRole(Long userId, Long issueId, RoleInIssue role);

    @Modifying
    @Transactional
    @Query("UPDATE UserIssue ui SET ui.revokedAt = :revokedAt " +
            "WHERE ui.user.id = :userId AND ui.issue.project.id = :projectId AND ui.revokedAt IS NULL")
    void revokeAllAssignmentsForUserInProject(@Param("userId") Long userId, @Param("projectId") Long projectId,
                                               @Param("revokedAt") Instant revokedAt);

    @Query("""
        SELECT ui FROM UserIssue ui
        JOIN FETCH ui.user
        JOIN FETCH ui.issue
        WHERE ui.issue.project.id = :projectId AND ui.revokedAt IS NULL
    """)
    List<UserIssue> findByIssueProjectIdWithUsers(@Param("projectId") Long projectId);
}
