package io.clamped.cloud.calendar;

import io.clamped.cloud.issue.Issue;
import io.clamped.cloud.issue.IssueRepository;
import io.clamped.cloud.user.UserPrincipal;
import io.clamped.cloud.userissue.UserIssue;
import io.clamped.cloud.userissue.UserIssueRepository;
import io.clamped.cloud.userproject.ProjectRole;
import io.clamped.cloud.userproject.UserProject;
import io.clamped.cloud.userproject.UserProjectRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CalendarService {

    private final UserProjectRepository userProjectRepository;
    private final UserIssueRepository userIssueRepository;
    private final IssueRepository issueRepository;

    public CalendarService(UserProjectRepository userProjectRepository,
                           UserIssueRepository userIssueRepository,
                           IssueRepository issueRepository) {
        this.userProjectRepository = userProjectRepository;
        this.userIssueRepository = userIssueRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    public List<CalendarVulnDto> getCalendarVulns(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.getId();

        Map<Long, Issue> seen = new LinkedHashMap<>();

        List<UserProject> memberships = userProjectRepository.findByUserId(userId);

        for (UserProject up : memberships) {
            Long projectId = up.getProject().getId();

            if (up.getRole() == ProjectRole.LEAD) {
                issueRepository.findIssuesByProjectId(projectId).stream()
                        .filter(i -> i.getDueAt() != null)
                        .forEach(i -> seen.putIfAbsent(i.getId(), i));
            } else {
                userIssueRepository.findByUserId(userId).stream()
                        .map(UserIssue::getIssue)
                        .filter(i -> i.getProject().getId().equals(projectId) && i.getDueAt() != null)
                        .forEach(i -> seen.putIfAbsent(i.getId(), i));
            }
        }

        return seen.values().stream()
                .map(i -> new CalendarVulnDto(
                        i.getId(),
                        i.getTitle(),
                        i.getSeverity(),
                        i.getStatus(),
                        i.getDueAt(),
                        i.getProject().getId(),
                        i.getProject().getName()
                ))
                .toList();
    }
}
