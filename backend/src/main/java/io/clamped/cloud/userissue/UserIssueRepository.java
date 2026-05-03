package io.clamped.cloud.userissue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserIssueRepository extends JpaRepository<UserIssue, UserIssueId> {

    List<UserIssue> findByUserId(Long userId);

    List<UserIssue> findByUserIdAndRole(Long userId, RoleInIssue role);

    Optional<UserIssue> findByUserIdAndIssueId(Long userId, Long issueId);

    boolean existsByUserIdAndIssueIdAndRole(Long userId, Long issueId, RoleInIssue role);

    boolean existsByIssueIdAndRole(Long issueId, RoleInIssue role);

    List<UserIssue> findByUserIdAndIssueProjectIdAndRole(Long userId, Long issueProjectId, RoleInIssue role);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserIssue ui WHERE ui.user.id = :userId AND ui.issue.project.id = :projectId")
    void deleteAllAssignmentsForUserInProject(Long userId, Long projectId);

    @Query("""
        SELECT ui FROM UserIssue ui
        JOIN FETCH ui.user
        JOIN FETCH ui.issue
        WHERE ui.issue.project.id = :projectId
    """)
    List<UserIssue> findByIssueProjectIdWithUsers(@Param("projectId") Long projectId);
}
