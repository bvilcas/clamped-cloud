package io.clamped.cloud.event;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EventGroupController {

    private final EventGroupService eventGroupService;

    public EventGroupController(EventGroupService eventGroupService) {
        this.eventGroupService = eventGroupService;
    }

    /** Public endpoint — validated by X-Api-Key header, no JWT required. */
    @PostMapping("/api/v1/events")
    public ResponseEntity<EventGroupDto> ingestEvent(
            @Valid @RequestBody IngestEventRequest request,
            @RequestHeader("X-Api-Key") String apiKey) {
        return ResponseEntity.ok(eventGroupService.ingestEvent(request, apiKey));
    }

    @GetMapping("/api/v1/projects/{projectId}/event-groups")
    public ResponseEntity<List<EventGroupDto>> getProjectEventGroups(
            @PathVariable Long projectId,
            @RequestParam(required = false) EventEnvironment environment,
            Authentication authentication) {
        return ResponseEntity.ok(eventGroupService.getProjectEventGroups(projectId, environment, authentication));
    }

    @PutMapping("/api/v1/event-groups/{id}/mute")
    public ResponseEntity<EventGroupDto> muteEventGroup(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(eventGroupService.muteEventGroup(id, authentication));
    }

    @PutMapping("/api/v1/event-groups/{id}/link-issue/{issueId}")
    public ResponseEntity<EventGroupDto> linkToIssue(
            @PathVariable Long id,
            @PathVariable Long issueId,
            Authentication authentication) {
        return ResponseEntity.ok(eventGroupService.linkToIssue(id, issueId, authentication));
    }
}
