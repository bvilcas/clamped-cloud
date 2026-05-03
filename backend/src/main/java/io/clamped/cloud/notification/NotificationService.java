package io.clamped.cloud.notification;

import io.clamped.cloud.user.UserPrincipal;
import io.clamped.cloud.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /** Create a single in-app notification and optionally send an email. */
    public void notify(Long recipientId, NotificationType type, String message,
                       Long relatedProjectId, Long relatedIssueId) {
        Notification n = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .message(message)
                .relatedProjectId(relatedProjectId)
                .relatedIssueId(relatedIssueId)
                .read(false)
                .build();
        notificationRepository.save(n);

        // Send email if the user has opted in
        userRepository.findById(recipientId).ifPresent(user -> {
            if (user.isEmailNotificationsEnabled()) {
                emailService.send(
                        user.getEmail(),
                        "Clamped — " + formatTypeLabel(type),
                        message
                );
            }
        });
    }

    /** Create the same notification for multiple recipients (e.g. all project leads). */
    public void notifyAll(List<Long> recipientIds, NotificationType type, String message,
                          Long relatedProjectId, Long relatedIssueId) {
        if (recipientIds == null || recipientIds.isEmpty()) return;
        List<Notification> notifications = recipientIds.stream()
                .distinct()
                .map(id -> Notification.builder()
                        .recipientId(id)
                        .type(type)
                        .message(message)
                        .relatedProjectId(relatedProjectId)
                        .relatedIssueId(relatedIssueId)
                        .read(false)
                        .build())
                .toList();
        notificationRepository.saveAll(notifications);

        // Email opted-in recipients
        String subject = "Clamped — " + formatTypeLabel(type);
        recipientIds.stream().distinct().forEach(id ->
                userRepository.findById(id).ifPresent(user -> {
                    if (user.isEmailNotificationsEnabled()) {
                        emailService.send(user.getEmail(), subject, message);
                    }
                })
        );
    }

    private String formatTypeLabel(NotificationType type) {
        return switch (type) {
            case ISSUE_ASSIGNED        -> "You've been assigned to an issue";
            case ISSUE_UNASSIGNED      -> "You've been removed from an issue";
            case ISSUE_REPORTED        -> "New issue reported";
            case ISSUE_STATUS_CHANGED  -> "Issue status changed";
            case ISSUE_SELF_ASSIGNED   -> "Member self-assigned to an issue";
            case ISSUE_SELF_REVOKED    -> "Member removed themselves from an issue";
            case PROJECT_ADDED         -> "You've been added to a project";
            case PROJECT_REMOVED       -> "You've been removed from a project";
        };
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getMyNotifications(Authentication auth) {
        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new NotificationDto(n.getId(), n.getType(), n.getMessage(),
                        n.getRelatedProjectId(), n.getRelatedIssueId(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Authentication auth) {
        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Authentication auth) {
        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();
        notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .ifPresent(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    @Transactional
    public void markAllAsRead(Authentication auth) {
        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();
        notificationRepository.markAllReadForUser(userId);
    }

    @Transactional
    public void deleteNotification(Long notificationId, Authentication auth) {
        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();
        notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .ifPresent(notificationRepository::delete);
    }

    @Transactional
    public void clearAll(Authentication auth) {
        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();
        notificationRepository.deleteAllForUser(userId);
    }
}
