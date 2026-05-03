package io.clamped.cloud.issue;

import io.clamped.cloud.event.EventGroup;
import io.clamped.cloud.event.EventGroupRepository;
import io.clamped.cloud.notification.NotificationService;
import io.clamped.cloud.notification.NotificationType;
import io.clamped.cloud.project.Project;
import io.clamped.cloud.project.ProjectRepository;
import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserPrincipal;
import io.clamped.cloud.user.UserRepository;
import io.clamped.cloud.userissue.RoleInIssue;
import io.clamped.cloud.userissue.UserIssue;
import io.clamped.cloud.userissue.UserIssueId;
import io.clamped.cloud.userissue.UserIssueRepository;
import io.clamped.cloud.userproject.ProjectRole;
import io.clamped.cloud.userproject.UserProject;
import io.clamped.cloud.userproject.UserProjectId;
import io.clamped.cloud.userproject.UserProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserProjectRepository userProjectRepository;
    private final UserRepository userRepository;
    private final UserIssueRepository userIssueRepository;
    private final NotificationService notificationService;
    private final EventGroupRepository eventGroupRepository;

    @Autowired
    public IssueService(IssueRepository issueRepository, ProjectRepository projectRepository,
                        UserProjectRepository userProjectRepository, UserRepository userRepository,
                        UserIssueRepository userIssueRepository, NotificationService notificationService,
                        EventGroupRepository eventGroupRepository) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.userProjectRepository = userProjectRepository;
        this.userRepository = userRepository;
        this.userIssueRepository = userIssueRepository;
        this.notificationService = notificationService;
        this.eventGroupRepository = eventGroupRepository;
    }

    @Transactional
    public void reportProjectIssue(Long projectId, IssueReportRequest request, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        UserProjectId membershipId = new UserProjectId(userId, projectId);
        if (!userProjectRepository.existsById(membershipId)) {
            throw new AccessDeniedException("You are not a member of this project");
        }

        Issue issue = Issue.builder()
                .title(request.title())
                .description(request.description())
                .type(request.type() != null ? request.type() : IssueType.OTHER)
                .cveId(request.cveId())
                .cweId(request.cweId())
                .severity(request.severity())
                .status(request.status())
                .updatedAt(null)
                .reportedAt(Instant.now())
                .dueAt(request.dueAt())
                .repository(request.repository())
                .commitHash(request.commitHash())
                .evidenceMachineId(request.evidenceMachineId())
                .evidenceLocalId(request.evidenceLocalId())
                .evidenceNote(request.evidenceNote())
                .project(project)
                .build();

        Issue saved = issueRepository.save(issue);

        UserIssue link = UserIssue.builder()
                .id(new UserIssueId(user.getId(), saved.getId()))
                .user(user)
                .issue(saved)
                .role(RoleInIssue.REPORTER)
                .selfAssigned(false)
                .assignedAt(Instant.now())
                .build();
        userIssueRepository.save(link);

        List<Long> memberIds = userProjectRepository.findAllByProjectId(projectId).stream()
                .map(up -> up.getUser().getId())
                .filter(id -> !id.equals(userId))
                .toList();
        notificationService.notifyAll(
                memberIds, NotificationType.ISSUE_REPORTED,
                user.getFirstname() + " " + user.getLastname() + " reported a new issue: '" + saved.getTitle() + "'",
                projectId, saved.getId()
        );
    }

    @Transactional
    public void updateProjectIssue(Long projectId, Long issueId, IssueUpdateRequest request, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        UserProjectId membershipId = new UserProjectId(userId, projectId);
        if (!userProjectRepository.existsById(membershipId)) {
            throw new AccessDeniedException("You are not a member of this project");
        }

        Issue issue = issueRepository.findIssueByProjectIdAndId(projectId, issueId)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found with ID: " + issueId));

        if (request.title() != null && !request.title().isBlank()) issue.setTitle(request.title());
        if (request.description() != null) issue.setDescription(request.description());
        if (request.type() != null) issue.setType(request.type());
        if (request.cveId() != null) issue.setCveId(request.cveId());
        if (request.cweId() != null) issue.setCweId(request.cweId());
        if (request.severity() != null) issue.setSeverity(request.severity());
        if (request.dueAt() != null) issue.setDueAt(request.dueAt());
        if (request.repository() != null) issue.setRepository(request.repository());
        if (request.commitHash() != null) issue.setCommitHash(request.commitHash());
        if (request.evidenceMachineId() != null) issue.setEvidenceMachineId(request.evidenceMachineId());
        if (request.evidenceLocalId() != null) issue.setEvidenceLocalId(request.evidenceLocalId());
        if (request.evidenceNote() != null) issue.setEvidenceNote(request.evidenceNote());

        issueRepository.save(issue);
    }

    @Transactional
    public void deleteProjectIssue(Long projectId, Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        if (!issue.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Issue does not belong to this project");
        }
        issueRepository.delete(issue);
    }

    @Transactional
    public void transitionProjectIssueStatus(ChangeIssueStatusRequest request, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        Issue issue = issueRepository.findById(request.issueId())
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        ProjectRole projectRole = userProjectRepository.findByUserIdAndProjectId(userId, request.projectId())
                .map(UserProject::getRole)
                .orElseThrow(() -> new AccessDeniedException("User not in this project"));

        RoleInIssue roleInIssue = userIssueRepository.findByUserIdAndIssueId(userId, issue.getId())
                .map(UserIssue::getRole)
                .orElse(null);

        IssueStatus current = issue.getStatus();
        IssueStatus target = request.newStatus();

        if (!isTransitionAllowed(projectRole, roleInIssue, current, target)) {
            throw new AccessDeniedException(
                    String.format("You cannot transition from %s to %s with your role", current, target));
        }

        issue.setStatus(target);
        issue.setUpdatedAt(Instant.now());
        issueRepository.save(issue);
    }

    @Transactional(readOnly = true)
    public List<Issue> getProjectIssues(Long projectId) {
        return issueRepository.findIssuesByProjectId(projectId);
    }

    public Issue getProjectIssueById(Long projectId, Long issueId) {
        return issueRepository.findIssueByProjectIdAndId(projectId, issueId)
                .orElseThrow(() -> new EntityNotFoundException("Issue with id '" + issueId + "' not found"));
    }

    public Issue getProjectIssueByTitle(Long projectId, String title) {
        return issueRepository.findIssueByProjectIdAndTitle(projectId, title)
                .orElseThrow(() -> new EntityNotFoundException("Issue with title '" + title + "' not found"));
    }

    public List<Issue> getProjectIssuesBySeverity(Long projectId, Severity severity) {
        return issueRepository.findIssuesByProjectIdAndSeverity(projectId, severity);
    }

    public List<Issue> getOverdueProjectIssues(Long projectId) {
        return issueRepository.findByDueAtBeforeAndStatusNotAndProjectId(Instant.now(), IssueStatus.VERIFIED, projectId);
    }

    @Transactional
    public Issue createFromEventGroup(Long groupId, Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();

        EventGroup group = eventGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Event group not found"));

        Long projectId = group.getProject().getId();
        ProjectRole role = userProjectRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProject::getRole)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this project"));
        if (role != ProjectRole.LEAD) {
            throw new AccessDeniedException("Only project LEADs can create issues from event groups");
        }

        String title = group.getMessage().length() > 100
                ? group.getMessage().substring(0, 100)
                : group.getMessage();

        String description = "Created from event group.\n"
                + "Source: " + group.getSource() + "\n"
                + "Environment: " + group.getEnvironment() + "\n"
                + "Occurrences: " + group.getCount() + "\n"
                + "First seen: " + group.getFirstSeen() + "\n"
                + "Last seen: " + group.getLastSeen();

        Issue issue = Issue.builder()
                .title(title)
                .description(description)
                .type(IssueType.OTHER)
                .status(IssueStatus.REPORTED)
                .reportedAt(Instant.now())
                .project(group.getProject())
                .build();

        Issue saved = issueRepository.save(issue);

        group.setLinkedIssue(saved);
        eventGroupRepository.save(group);

        return saved;
    }

    private boolean isTransitionAllowed(ProjectRole projectRole, RoleInIssue roleInIssue,
                                        IssueStatus current, IssueStatus target) {
        if (projectRole == ProjectRole.LEAD) return true;

        if (projectRole == ProjectRole.PROGRAMMER) {
            if (roleInIssue != RoleInIssue.ASSIGNEE) return false;
            return (current == IssueStatus.REPORTED && target == IssueStatus.IN_PROGRESS) ||
                    (current == IssueStatus.IN_PROGRESS && target == IssueStatus.PATCHED);
        }

        if (projectRole == ProjectRole.TESTER) {
            if (roleInIssue != RoleInIssue.VERIFIER) return false;
            return (current == IssueStatus.PATCHED && target == IssueStatus.UNDER_REVIEW) ||
                    (current == IssueStatus.UNDER_REVIEW && target == IssueStatus.VERIFIED);
        }

        return false;
    }
}
