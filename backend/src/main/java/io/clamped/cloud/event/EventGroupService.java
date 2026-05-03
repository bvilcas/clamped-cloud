package io.clamped.cloud.event;

import io.clamped.cloud.issue.Issue;
import io.clamped.cloud.issue.IssueRepository;
import io.clamped.cloud.issue.Severity;
import io.clamped.cloud.project.Project;
import io.clamped.cloud.project.ProjectRepository;
import io.clamped.cloud.user.UserPrincipal;
import io.clamped.cloud.userproject.ProjectRole;
import io.clamped.cloud.userproject.UserProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
public class EventGroupService {

    private final EventGroupRepository eventGroupRepository;
    private final ProjectRepository projectRepository;
    private final UserProjectRepository userProjectRepository;
    private final IssueRepository issueRepository;

    public EventGroupService(EventGroupRepository eventGroupRepository,
                             ProjectRepository projectRepository,
                             UserProjectRepository userProjectRepository,
                             IssueRepository issueRepository) {
        this.eventGroupRepository = eventGroupRepository;
        this.projectRepository = projectRepository;
        this.userProjectRepository = userProjectRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional
    public EventGroupDto ingestEvent(IngestEventRequest request, String apiKey) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        if (!isValidApiKey(project.getApiKey(), apiKey)) {
            throw new AccessDeniedException("Invalid API key");
        }

        String fingerprint = request.fingerprint() != null && !request.fingerprint().isBlank()
                ? request.fingerprint()
                : computeFingerprint(request.projectId(), request.source(), request.environment(), request.message());

        String tagsStr = request.tags() != null ? String.join(",", request.tags()) : null;

        EventGroup group = eventGroupRepository
                .findByProjectIdAndFingerprint(request.projectId(), fingerprint)
                .orElse(null);

        if (group != null) {
            group.setCount(group.getCount() + 1);
            group.setLastSeen(Instant.now());
        } else {
            Instant now = Instant.now();
            group = EventGroup.builder()
                    .fingerprint(fingerprint)
                    .message(request.message())
                    .source(request.source())
                    .environment(request.environment())
                    .severity(request.severity() != null ? request.severity() : Severity.MEDIUM)
                    .count(1L)
                    .firstSeen(now)
                    .lastSeen(now)
                    .muted(false)
                    .tags(tagsStr)
                    .exceptionClass(request.exceptionClass())
                    .stacktrace(request.stacktrace())
                    .sourceFile(request.sourceFile())
                    .sourceLine(request.sourceLine())
                    .sourceMethod(request.sourceMethod())
                    .project(project)
                    .build();
        }

        EventGroup saved = eventGroupRepository.save(group);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<EventGroupDto> getProjectEventGroups(Long projectId, EventEnvironment environment, Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();

        userProjectRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this project"));

        List<EventGroup> groups = environment != null
                ? eventGroupRepository.findByProjectIdAndEnvironmentOrderByLastSeenDesc(projectId, environment)
                : eventGroupRepository.findByProjectIdOrderByLastSeenDesc(projectId);

        return groups.stream().map(this::toDto).toList();
    }

    @Transactional
    public EventGroupDto muteEventGroup(Long groupId, Authentication authentication) {
        EventGroup group = eventGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Event group not found"));

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
        requireLead(userId, group.getProject().getId());

        group.setMuted(!group.isMuted());
        return toDto(eventGroupRepository.save(group));
    }

    @Transactional
    public EventGroupDto linkToIssue(Long groupId, Long issueId, Authentication authentication) {
        EventGroup group = eventGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Event group not found"));

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
        requireLead(userId, group.getProject().getId());

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Issue not found"));

        group.setLinkedIssue(issue);
        return toDto(eventGroupRepository.save(group));
    }

    private void requireLead(Long userId, Long projectId) {
        ProjectRole role = userProjectRepository.findByUserIdAndProjectId(userId, projectId)
                .map(up -> up.getRole())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this project"));
        if (role != ProjectRole.LEAD) {
            throw new AccessDeniedException("Only project LEADs can perform this action");
        }
    }

    private boolean isValidApiKey(String storedKey, String providedKey) {
        if (storedKey == null || providedKey == null) return false;
        byte[] stored = storedKey.getBytes(StandardCharsets.UTF_8);
        byte[] provided = providedKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(stored, provided);
    }

    private String computeFingerprint(Long projectId, EventSource source, EventEnvironment environment, String message) {
        try {
            String raw = projectId + "|" + source + "|" + environment + "|" + message;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private EventGroupDto toDto(EventGroup g) {
        return new EventGroupDto(
                g.getId(),
                g.getFingerprint(),
                g.getMessage(),
                g.getSource(),
                g.getEnvironment(),
                g.getSeverity(),
                g.getCount(),
                g.getFirstSeen(),
                g.getLastSeen(),
                g.isMuted(),
                g.getTags(),
                g.getExceptionClass(),
                g.getStacktrace(),
                g.getSourceFile(),
                g.getSourceLine(),
                g.getSourceMethod(),
                g.getLinkedIssue() != null ? g.getLinkedIssue().getId() : null,
                g.getProject().getId()
        );
    }
}
