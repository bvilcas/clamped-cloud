package io.clamped.cloud.message;

import io.clamped.cloud.backendconfig.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PreAuthorize("@projectSecurity.isProjectMember(authentication, #request.projectId())")
    @PostMapping
    public ResponseEntity<ApiResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        MessageDto message = messageService.sendMessage(request, authentication);
        return ResponseEntity.ok(new ApiResponse(true, "Message sent", message));
    }

    @PreAuthorize("@projectSecurity.isProjectMember(authentication, #projectId)")
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse> getProjectMessages(@PathVariable Long projectId) {
        return ResponseEntity.ok(new ApiResponse(true, "Messages retrieved",
                messageService.getProjectMessages(projectId)));
    }
}
