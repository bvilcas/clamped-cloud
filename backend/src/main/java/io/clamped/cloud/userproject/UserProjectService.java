package io.clamped.cloud.userproject;

import io.clamped.cloud.issue.Issue;
import io.clamped.cloud.issue.IssueRepository;
import io.clamped.cloud.issue.IssueStatus;
import io.clamped.cloud.notification.NotificationService;
import io.clamped.cloud.notification.NotificationType;
import io.clamped.cloud.project.Project;
import io.clamped.cloud.project.ProjectRepository;
import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserPrincipal;
import io.clamped.cloud.user.UserRepository;
import io.clamped.cloud.userissue.RoleInIssue;
import io.clamped.cloud.userissue.UserIssue;
import io.clamped.cloud.userissue.UserIssueRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class UserProjectService {

    private final UserProjectRepository userProjectRepository;
    private final UserIssueRepository userIssueRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;

    public UserProjectService(UserProjectRepository userProjectRepository,
                               UserIssueRepository userIssueRepository,
                               UserRepository userRepository,
                               IssueRepository issueRepository,
                               ProjectRepository projectRepository,
                               NotificationService notificationService) {
        this.userProjectRepository = userProjectRepository;
        this.userIssueRepository = userIssueRepository;
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryDto> getMyProjects(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return userProjectRepository.findByUserId(userId).stream()
                .map(link -> {
                    Project p = link.getProject();
                    return new ProjectSummaryDto(
                            p.getId(),
                            p.getName(),
                            p.getDescription(),
                            p.getCreatedAt(),
                            p.getUpdatedAt(),
                            link.getRole().name()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectSummaryDto getMyProjectById(Authentication authentication, Long projectId) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserProject userProject = userProjectRepository
                .findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found for this user"));

        Project p = userProject.getProject();

        return new ProjectSummaryDto(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                userProject.getRole().name()
        );
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryDto> getMyProjectByName(Authentication authentication, String name) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<ProjectSummaryDto> results = userProjectRepository.findByUserId(userId).stream()
                .filter(link -> link.getProject().getName().equalsIgnoreCase(name))
                .map(link -> {
                    Project p = link.getProject();
                    return new ProjectSummaryDto(
                            p.getId(),
                            p.getName(),
                            p.getDescription(),
                            p.getCreatedAt(),
                            p.getUpdatedAt(),
                            link.getRole().name()
                    );
                })
                .toList();

        if (results.isEmpty()) {
            throw new EntityNotFoundException("No projects found with name: " + name);
        }

        return results;
    }

    @Transactional
    public RoleCheckerResponse changeUserRoleInProject(ChangeProjectRoleCommand request) {
        Long projectId = request.projectId();
        Long userId = request.userId();

        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserProject userProject = userProjectRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new IllegalStateException("User is not part of this project"));

        ProjectRole oldRole = userProject.getRole();
        ProjectRole newRole = request.newRole();

        if (oldRole == newRole) {
            throw new IllegalStateException("User already has this project role");
        }

        // PROGRAMMER → TESTER: remove ASSIGNEE issue links
        if (oldRole == ProjectRole.PROGRAMMER && newRole == ProjectRole.TESTER) {
            List<UserIssue> assigneeLinks =
                    userIssueRepository.findByUserIdAndIssueProjectIdAndRole(userId, projectId, RoleInIssue.ASSIGNEE);

            for (UserIssue link : assigneeLinks) {
                Issue issue = link.getIssue();
                userIssueRepository.delete(link);
                boolean hasOtherAssignees = userIssueRepository.existsByIssueIdAndRole(issue.getId(), RoleInIssue.ASSIGNEE);
                if (!hasOtherAssignees && issue.getStatus() == IssueStatus.IN_PROGRESS) {
                    issue.setStatus(IssueStatus.REPORTED);
                    issue.setUpdatedAt(Instant.now());
                    issueRepository.save(issue);
                }
            }
        }

        // TESTER → PROGRAMMER: remove VERIFIER issue links
        if (oldRole == ProjectRole.TESTER && newRole == ProjectRole.PROGRAMMER) {
            List<UserIssue> verifierLinks =
                    userIssueRepository.findByUserIdAndIssueProjectIdAndRole(userId, projectId, RoleInIssue.VERIFIER);

            for (UserIssue link : verifierLinks) {
                Issue issue = link.getIssue();
                userIssueRepository.delete(link);
                boolean hasOtherVerifiers = userIssueRepository.existsByIssueIdAndRole(issue.getId(), RoleInIssue.VERIFIER);
                if (!hasOtherVerifiers && issue.getStatus() == IssueStatus.UNDER_REVIEW) {
                    issue.setStatus(IssueStatus.IN_PROGRESS);
                    issue.setUpdatedAt(Instant.now());
                    issueRepository.save(issue);
                }
            }
        }

        // LEAD → TESTER: remove ASSIGNEE links; keep VERIFIER
        // LEAD → PROGRAMMER: remove VERIFIER links; keep ASSIGNEE
        if (oldRole == ProjectRole.LEAD && newRole != ProjectRole.LEAD) {
            List<UserIssue> assigneeLinks =
                    userIssueRepository.findByUserIdAndIssueProjectIdAndRole(userId, projectId, RoleInIssue.ASSIGNEE);
            List<UserIssue> verifierLinks =
                    userIssueRepository.findByUserIdAndIssueProjectIdAndRole(userId, projectId, RoleInIssue.VERIFIER);

            if (newRole == ProjectRole.TESTER) {
                for (UserIssue link : assigneeLinks) {
                    Issue issue = link.getIssue();
                    userIssueRepository.delete(link);
                    boolean hasOtherAssignees = userIssueRepository.existsByIssueIdAndRole(issue.getId(), RoleInIssue.ASSIGNEE);
                    if (!hasOtherAssignees && issue.getStatus() == IssueStatus.IN_PROGRESS) {
                        issue.setStatus(IssueStatus.REPORTED);
                        issue.setUpdatedAt(Instant.now());
                        issueRepository.save(issue);
                    }
                }
            }

            if (newRole == ProjectRole.PROGRAMMER) {
                for (UserIssue link : verifierLinks) {
                    Issue issue = link.getIssue();
                    userIssueRepository.delete(link);
                    boolean hasOtherVerifiers = userIssueRepository.existsByIssueIdAndRole(issue.getId(), RoleInIssue.VERIFIER);
                    if (!hasOtherVerifiers && issue.getStatus() == IssueStatus.UNDER_REVIEW) {
                        issue.setStatus(IssueStatus.IN_PROGRESS);
                        issue.setUpdatedAt(Instant.now());
                        issueRepository.save(issue);
                    }
                }
            }
        }

        if (newRole == ProjectRole.LEAD) {
            return new RoleCheckerResponse(
                    true,
                    "You are about to promote this user to LEAD. This will add an additional project lead.",
                    "PROMOTING_MEMBER_LEAD"
            );
        }

        userProject.setRole(newRole);
        userProjectRepository.save(userProject);
        return new RoleCheckerResponse(false, null);
    }

    public List<ProjectMemberResponse> getProjectMembers(Long projectId) {
        return userProjectRepository.findAllByProjectId(projectId)
                .stream()
                .map(up -> new ProjectMemberResponse(
                        up.getUser().getId(),
                        up.getUser().getFirstname(),
                        up.getUser().getLastname(),
                        up.getRole()
                ))
                .toList();
    }

    @Transactional
    public RoleCheckerResponse addMemberToProject(AddMemberRequest request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean alreadyMember = userProjectRepository.findByUserIdAndProjectId(user.getId(), request.projectId()).isPresent();
        if (alreadyMember) {
            throw new IllegalStateException("User is already a member of this project");
        }

        ProjectRole role = request.role();

        if (role == ProjectRole.LEAD) {
            return new RoleCheckerResponse(
                    true,
                    "You are adding a new LEAD to this project. This will grant them full permissions.",
                    "ADDING_MEMBER_LEAD"
            );
        }

        UserProject userProject = new UserProject();
        userProject.setUser(user);
        userProject.setProject(project);
        userProject.setRole(role);
        userProjectRepository.save(userProject);

        notificationService.notify(
                user.getId(), NotificationType.PROJECT_ADDED,
                "You have been added to project '" + project.getName() + "' as " + role.name().toLowerCase(),
                project.getId(), null
        );
        return new RoleCheckerResponse(false, "Member successfully added.");
    }

    @Transactional(readOnly = true)
    public RoleCheckerResponse validateRemoveMember(RemoveMemberRequest request) {
        Long userId = request.userId();
        Long projectId = request.projectId();

        UserProject userProject = userProjectRepository
                .findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this project"));

        ProjectRole removedRole = userProject.getRole();

        List<UserProject> remainingMembers =
                userProjectRepository.findAllByProjectId(projectId)
                        .stream()
                        .filter(up -> !up.getUser().getId().equals(userId))
                        .toList();

        if (removedRole == ProjectRole.LEAD) {
            boolean hasOtherLeads = remainingMembers.stream()
                    .anyMatch(up -> up.getRole() == ProjectRole.LEAD);

            if (!hasOtherLeads) {
                return new RoleCheckerResponse(
                        true,
                        "Removing this member will leave the project without a lead.",
                        "REMOVING_MEMBER_LEAD"
                );
            }
        }

        return new RoleCheckerResponse(false, "SAFE_TO_DELETE");
    }

    @Transactional
    public void removeMemberFromProject(RemoveMemberRequest request) {
        UserProject link = userProjectRepository
                .findByUserIdAndProjectId(request.userId(), request.projectId())
                .orElseThrow(() -> new IllegalArgumentException("User not in project"));

        String projectName = link.getProject().getName();
        Long projectId = link.getProject().getId();

        userIssueRepository.deleteAllAssignmentsForUserInProject(request.userId(), request.projectId());
        userProjectRepository.delete(link);

        notificationService.notify(
                request.userId(), NotificationType.PROJECT_REMOVED,
                "You have been removed from project '" + projectName + "'",
                projectId, null
        );
    }

    @Transactional(readOnly = true)
    public RoleCheckerResponse validateSelfRemove(Long projectId, Authentication auth) {
        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();

        UserProject userProject = userProjectRepository
                .findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this project"));

        ProjectRole role = userProject.getRole();

        List<UserProject> remainingMembers =
                userProjectRepository.findAllByProjectId(projectId)
                        .stream()
                        .filter(up -> !up.getUser().getId().equals(userId))
                        .toList();

        if (remainingMembers.isEmpty()) {
            return new RoleCheckerResponse(
                    true,
                    "You are the last member. Leaving will delete the project.",
                    "SELF_REMOVE_LAST_MEMBER"
            );
        }

        boolean hasOtherLeads = remainingMembers.stream()
                .anyMatch(up -> up.getRole() == ProjectRole.LEAD);

        if (role == ProjectRole.LEAD && !hasOtherLeads) {
            return new RoleCheckerResponse(
                    true,
                    "You are the last lead. Promote another member first.",
                    "SELF_REMOVE_LAST_LEAD"
            );
        }

        return new RoleCheckerResponse(false, "SAFE_TO_DELETE");
    }

    @Transactional
    public void selfRemoveFromProject(Long projectId, Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with ID " + projectId));

        UserProject link = userProjectRepository
                .findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Not a member"));

        boolean isLead = link.getRole() == ProjectRole.LEAD;
        long totalMembers = userProjectRepository.countByProject(project);

        if (!isLead) {
            userProjectRepository.delete(link);
            userIssueRepository.deleteAllAssignmentsForUserInProject(userId, projectId);
            return;
        }

        userProjectRepository.delete(link);
        userIssueRepository.deleteAllAssignmentsForUserInProject(userId, projectId);

        if (totalMembers == 1) {
            projectRepository.delete(project);
        }
    }
}
