package io.clamped.cloud.userissue;

import io.clamped.cloud.issue.*;
import io.clamped.cloud.notification.NotificationService;
import io.clamped.cloud.notification.NotificationType;
import io.clamped.cloud.userproject.ProjectRole;
import io.clamped.cloud.userproject.UserProject;
import io.clamped.cloud.userproject.UserProjectRepository;
import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserPrincipal;
import io.clamped.cloud.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserIssueService {

    private final UserIssueRepository userIssueRepository;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final UserProjectRepository userProjectRepository;
    private final NotificationService notificationService;

    @Autowired
    public UserIssueService(UserIssueRepository userIssueRepository, UserRepository userRepository,
                            IssueRepository issueRepository,
                            UserProjectRepository userProjectRepository, NotificationService notificationService) {
        this.userIssueRepository = userIssueRepository;
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
        this.userProjectRepository = userProjectRepository;
        this.notificationService = notificationService;
    }

    private List<Long> getLeadIds(Long projectId, Long excludeUserId) {
        return userProjectRepository.findAllByProjectId(projectId).stream()
                .filter(up -> up.getRole() == ProjectRole.LEAD && !up.getUser().getId().equals(excludeUserId))
                .map(up -> up.getUser().getId())
                .toList();
    }

    private static RoleInIssue oppositeOf(RoleInIssue role) {
        return role == RoleInIssue.ASSIGNEE ? RoleInIssue.VERIFIER : RoleInIssue.ASSIGNEE;
    }

    // Blocks self-certification: a user who ever held the opposite of ASSIGNEE/VERIFIER
    // on this issue - even a revoked assignment - can't take the other side of it.
    private void assertNoRoleConflict(Long userId, Long issueId, RoleInIssue role) {
        if (role != RoleInIssue.ASSIGNEE && role != RoleInIssue.VERIFIER) return;
        RoleInIssue conflictingRole = oppositeOf(role);
        if (userIssueRepository.existsByUserIdAndIssueIdAndRole(userId, issueId, conflictingRole)) {
            throw new AccessDeniedException(
                    "This user previously held the " + conflictingRole.name().toLowerCase()
                            + " role on this issue and cannot be assigned as " + role.name().toLowerCase());
        }
    }

    @Transactional
    public void selfAssignToIssue(IssueSelfAssignRevoke selfAssign, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Issue issue = issueRepository.getIssuesById(selfAssign.issueId());

        userProjectRepository.findByUserIdAndProjectId(user.getId(), selfAssign.projectId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this project"));

        RoleInIssue roleInIssue = selfAssign.role();
        if (roleInIssue != RoleInIssue.ASSIGNEE && roleInIssue != RoleInIssue.VERIFIER) {
            throw new IllegalArgumentException("You must self-assign as either ASSIGNEE or VERIFIER");
        }

        if (userIssueRepository.existsByUserIdAndIssueIdAndRevokedAtIsNull(user.getId(), issue.getId())) {
            throw new IllegalStateException("You are already assigned to this issue");
        }
        assertNoRoleConflict(user.getId(), issue.getId(), roleInIssue);

        UserIssue link = UserIssue.builder()
                .user(user)
                .issue(issue)
                .role(roleInIssue)
                .selfAssigned(true)
                .assignedAt(Instant.now())
                .build();

        userIssueRepository.save(link);

        switch (roleInIssue) {
            case ASSIGNEE -> {
                if (issue.getStatus() == IssueStatus.REPORTED) {
                    issue.setStatus(IssueStatus.IN_PROGRESS);
                    issue.setUpdatedAt(Instant.now());
                }
            }
            case VERIFIER -> {
                if (issue.getStatus() == IssueStatus.PATCHED) {
                    issue.setStatus(IssueStatus.UNDER_REVIEW);
                    issue.setUpdatedAt(Instant.now());
                }
            }
        }
        issueRepository.save(issue);

        notificationService.notifyAll(
                getLeadIds(issue.getProject().getId(), user.getId()),
                NotificationType.ISSUE_SELF_ASSIGNED,
                user.getFirstname() + " " + user.getLastname() + " self-assigned to '" + issue.getTitle() + "' as " + roleInIssue.name().toLowerCase(),
                issue.getProject().getId(), issue.getId()
        );
    }

    @Transactional
    public void selfRevokeFromIssue(IssueSelfAssignRevoke selfRevoke, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Issue issue = issueRepository.getIssuesById(selfRevoke.issueId());

        Long projectId = issue.getProject().getId();

        userProjectRepository.findByUserIdAndProjectId(user.getId(), projectId)
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this project"));

        UserIssue link = userIssueRepository
                .findByUserIdAndIssueIdAndRevokedAtIsNull(user.getId(), issue.getId())
                .orElseThrow(() -> new IllegalStateException("You are not assigned to this issue"));

        RoleInIssue roleInIssue = link.getRole();
        link.setRevokedAt(Instant.now());
        userIssueRepository.save(link);

        switch (roleInIssue) {
            case ASSIGNEE -> {
                boolean hasOtherAssignees = userIssueRepository.existsByIssueIdAndRoleAndRevokedAtIsNull(selfRevoke.issueId(), RoleInIssue.ASSIGNEE);
                if (!hasOtherAssignees && issue.getStatus() == IssueStatus.IN_PROGRESS) {
                    issue.setStatus(IssueStatus.REPORTED);
                    issue.setUpdatedAt(Instant.now());
                }
            }
            case VERIFIER -> {
                boolean hasOtherVerifiers = userIssueRepository.existsByIssueIdAndRoleAndRevokedAtIsNull(selfRevoke.issueId(), RoleInIssue.VERIFIER);
                if (!hasOtherVerifiers && issue.getStatus() == IssueStatus.UNDER_REVIEW) {
                    issue.setStatus(IssueStatus.PATCHED);
                    issue.setUpdatedAt(Instant.now());
                }
            }
        }
        issueRepository.save(issue);

        notificationService.notifyAll(
                getLeadIds(issue.getProject().getId(), user.getId()),
                NotificationType.ISSUE_SELF_REVOKED,
                user.getFirstname() + " " + user.getLastname() + " unassigned themselves from '" + issue.getTitle() + "'",
                issue.getProject().getId(), issue.getId()
        );
    }

    @Transactional
    public User assignUserToIssue(IssueAssignAction action) {
        Issue issue = issueRepository.findById(action.issueId())
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        User user = userRepository.findById(action.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Long projectId = issue.getProject().getId();

        userProjectRepository
                .findByUserIdAndProjectId(user.getId(), projectId)
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this project"));

        RoleInIssue issueRole = action.role();

        if (userIssueRepository.existsByUserIdAndIssueIdAndRevokedAtIsNull(user.getId(), issue.getId())) {
            throw new IllegalStateException("User is already assigned to this issue");
        }
        assertNoRoleConflict(user.getId(), issue.getId(), issueRole);

        UserIssue link = UserIssue.builder()
                .user(user)
                .issue(issue)
                .role(issueRole)
                .assignedAt(Instant.now())
                .build();

        userIssueRepository.save(link);

        switch (issueRole) {
            case ASSIGNEE -> {
                if (issue.getStatus() == IssueStatus.REPORTED) {
                    issue.setStatus(IssueStatus.IN_PROGRESS);
                    issue.setUpdatedAt(Instant.now());
                }
            }
            case VERIFIER -> {
                if (issue.getStatus() == IssueStatus.PATCHED) {
                    issue.setStatus(IssueStatus.UNDER_REVIEW);
                    issue.setUpdatedAt(Instant.now());
                }
            }
        }
        issueRepository.save(issue);

        notificationService.notify(
                user.getId(), NotificationType.ISSUE_ASSIGNED,
                "You were assigned as " + issueRole.name().toLowerCase() + " to '" + issue.getTitle() + "' in '" + issue.getProject().getName() + "'",
                projectId, issue.getId()
        );
        return user;
    }

    @Transactional
    public User revokeUserFromIssue(IssueRevokeAction action) {
        Issue issue = issueRepository.findById(action.issueId())
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        User user = userRepository.findById(action.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserIssue link = userIssueRepository.findByUserIdAndIssueIdAndRevokedAtIsNull(action.userId(), action.issueId())
                .orElseThrow(() -> new EntityNotFoundException("User is not assigned to this issue"));

        RoleInIssue role = link.getRole();
        link.setRevokedAt(Instant.now());
        userIssueRepository.save(link);

        if (role == RoleInIssue.ASSIGNEE) {
            boolean hasOtherAssignees = userIssueRepository.existsByIssueIdAndRoleAndRevokedAtIsNull(action.issueId(), RoleInIssue.ASSIGNEE);
            if (!hasOtherAssignees && issue.getStatus() == IssueStatus.IN_PROGRESS) {
                issue.setStatus(IssueStatus.REPORTED);
            }
        }

        if (role == RoleInIssue.VERIFIER) {
            boolean hasOtherVerifiers = userIssueRepository.existsByIssueIdAndRoleAndRevokedAtIsNull(action.issueId(), RoleInIssue.VERIFIER);
            if (!hasOtherVerifiers && issue.getStatus() == IssueStatus.UNDER_REVIEW) {
                issue.setStatus(IssueStatus.PATCHED);
            }
        }
        issueRepository.save(issue);

        notificationService.notify(
                user.getId(), NotificationType.ISSUE_UNASSIGNED,
                "You were unassigned from '" + issue.getTitle() + "' in '" + issue.getProject().getName() + "'",
                issue.getProject().getId(), issue.getId()
        );
        return user;
    }

    private IssueWithProjectDto toDto(Issue i) {
        return new IssueWithProjectDto(
                i.getId(), i.getTitle(), i.getDescription(), i.getType(),
                i.getSeverity(), i.getStatus(),
                i.getUpdatedAt(), i.getReportedAt(), i.getDueAt(),
                i.getPatchedAt(), i.getVerifiedAt(), i.getRepository(), i.getCommitHash(),
                i.getEventApp(), i.getEventHost(), i.getEventType(), i.getEventExtra(),
                i.getProject().getId(), i.getProject().getName()
        );
    }

    public List<IssueWithProjectDto> getReportedByMe(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userIssueRepository.findByUserIdAndRoleAndRevokedAtIsNull(userId, RoleInIssue.REPORTER).stream()
                .map(ui -> toDto(ui.getIssue())).collect(Collectors.toList());
    }

    public List<Issue> getReportedByMeInProject(Long projectId, Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userIssueRepository.findByUserIdAndRoleAndRevokedAtIsNull(userId, RoleInIssue.REPORTER).stream()
                .map(UserIssue::getIssue)
                .filter(i -> i.getProject().getId().equals(projectId))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<IssueWithProjectDto> getAssignedToMe(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userIssueRepository.findByUserIdAndRoleAndRevokedAtIsNull(userId, RoleInIssue.ASSIGNEE).stream()
                .map(ui -> toDto(ui.getIssue())).toList();
    }

    public List<Issue> getAssignedToMeInProject(Long projectId, Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userIssueRepository.findByUserIdAndRoleAndRevokedAtIsNull(userId, RoleInIssue.ASSIGNEE).stream()
                .map(UserIssue::getIssue)
                .filter(i -> i.getProject().getId().equals(projectId))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<IssueWithProjectDto> getVerifiedByMe(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userIssueRepository.findByUserIdAndRoleAndRevokedAtIsNull(userId, RoleInIssue.VERIFIER).stream()
                .map(ui -> toDto(ui.getIssue())).toList();
    }

    public List<Issue> getVerifiedByMeInProject(Long projectId, Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userIssueRepository.findByUserIdAndRoleAndRevokedAtIsNull(userId, RoleInIssue.VERIFIER).stream()
                .map(UserIssue::getIssue)
                .filter(i -> i.getProject().getId().equals(projectId))
                .collect(Collectors.toList());
    }

    public List<Issue> getReportedByUserInProject(UserProjectRelationshipRequest request) {
        userRepository.findById(request.userId()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userIssueRepository.findByUserIdAndRoleAndRevokedAtIsNull(request.userId(), RoleInIssue.REPORTER).stream()
                .map(UserIssue::getIssue)
                .filter(i -> i.getProject().getId().equals(request.projectId()))
                .collect(Collectors.toList());
    }

    public List<Issue> getAssignedToUserInProject(UserProjectRelationshipRequest request) {
        userRepository.findById(request.userId()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userIssueRepository.findByUserIdAndRoleAndRevokedAtIsNull(request.userId(), RoleInIssue.ASSIGNEE).stream()
                .map(UserIssue::getIssue)
                .filter(i -> i.getProject().getId().equals(request.projectId()))
                .collect(Collectors.toList());
    }

    public List<Issue> getVerifiedByUserInProject(UserProjectRelationshipRequest request) {
        userRepository.findById(request.userId()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return userIssueRepository.findByUserIdAndRoleAndRevokedAtIsNull(request.userId(), RoleInIssue.VERIFIER).stream()
                .map(UserIssue::getIssue)
                .filter(i -> i.getProject().getId().equals(request.projectId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueAssignmentDto> getProjectAssignments(Long projectId) {
        List<UserIssue> uis = userIssueRepository.findByIssueProjectIdWithUsers(projectId);

        Map<Long, List<UserIssue>> grouped = uis.stream()
                .collect(Collectors.groupingBy(ui -> ui.getIssue().getId(), LinkedHashMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Issue i = entry.getValue().get(0).getIssue();
                    List<AssignmentEntry> assignments = entry.getValue().stream()
                            .map(ui -> new AssignmentEntry(
                                    ui.getUser().getId(),
                                    ui.getUser().getFirstname(),
                                    ui.getUser().getLastname(),
                                    ui.getRole(),
                                    ui.getAssignedAt(),
                                    ui.isSelfAssigned()
                            ))
                            .collect(Collectors.toList());
                    return new IssueAssignmentDto(
                            i.getId(), i.getTitle(), i.getDescription(), i.getType(),
                            i.getSeverity(), i.getStatus(),
                            i.getReportedAt(), i.getDueAt(), i.getPatchedAt(), i.getVerifiedAt(),
                            i.getRepository(), i.getCommitHash(),
                            i.getEventApp(), i.getEventHost(), i.getEventType(), i.getEventExtra(),
                            assignments
                    );
                })
                .collect(Collectors.toList());
    }
}
