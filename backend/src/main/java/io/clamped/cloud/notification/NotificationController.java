package io.clamped.cloud.notification;

import io.clamped.cloud.backendconfig.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getMyNotifications(Authentication auth) {
        List<NotificationDto> notifications = notificationService.getMyNotifications(auth);
        return ResponseEntity.ok(new ApiResponse(true, "Notifications retrieved", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse> getUnreadCount(Authentication auth) {
        long count = notificationService.getUnreadCount(auth);
        return ResponseEntity.ok(new ApiResponse(true, "Unread count retrieved", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse> markAsRead(@PathVariable Long id, Authentication auth) {
        notificationService.markAsRead(id, auth);
        return ResponseEntity.ok(new ApiResponse(true, "Notification marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse> markAllAsRead(Authentication auth) {
        notificationService.markAllAsRead(auth);
        return ResponseEntity.ok(new ApiResponse(true, "All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteNotification(@PathVariable Long id, Authentication auth) {
        notificationService.deleteNotification(id, auth);
        return ResponseEntity.ok(new ApiResponse(true, "Notification deleted"));
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<ApiResponse> clearAll(Authentication auth) {
        notificationService.clearAll(auth);
        return ResponseEntity.ok(new ApiResponse(true, "All notifications cleared"));
    }
}
