package io.clamped.cloud.issue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    Optional<Issue> findIssueByProjectIdAndId(Long projectId, Long issueId);

    List<Issue> findIssuesByProjectId(Long projectId);

    Optional<Issue> findIssueByProjectIdAndTitle(Long projectId, String title);

    List<Issue> findByDueAtBeforeAndStatusNotAndProjectId(Instant dueAtBefore, IssueStatus status, Long projectId);

    List<Issue> findIssuesByProjectIdAndSeverity(Long projectId, Severity severity);

    // Admin
    Optional<Issue> findIssueById(Long issueId);

    Optional<Issue> findIssueByTitle(String title);

    List<Issue> findIssuesBySeverity(Severity severity);

    List<Issue> findByDueAtBeforeAndStatusNot(Instant now, IssueStatus issueStatus);

    Issue getIssuesById(Long id);
}
