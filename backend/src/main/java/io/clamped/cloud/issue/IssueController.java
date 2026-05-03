package io.clamped.cloud.issue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/issues")
public class IssueController {

    private final IssueService issueService;

    @Autowired
    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping("/report/{projectId}")
    public ResponseEntity<Void> reportIssue(
            @PathVariable Long projectId,
            @RequestBody IssueReportRequest request,
            Authentication authentication) {
        issueService.reportProjectIssue(projectId, request, authentication);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/update/{projectId}/{issueId}")
    public ResponseEntity<Void> updateIssue(
            @PathVariable Long projectId,
            @PathVariable Long issueId,
            @RequestBody IssueUpdateRequest request,
            Authentication authentication) {
        issueService.updateProjectIssue(projectId, issueId, request, authentication);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{projectId}/{issueId}")
    public ResponseEntity<Void> deleteIssue(
            @PathVariable Long projectId,
            @PathVariable Long issueId) {
        issueService.deleteProjectIssue(projectId, issueId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/status")
    public ResponseEntity<Void> transitionStatus(
            @RequestBody ChangeIssueStatusRequest request,
            Authentication authentication) {
        issueService.transitionProjectIssueStatus(request, authentication);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all/{projectId}")
    public ResponseEntity<List<IssueDto>> getProjectIssues(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                issueService.getProjectIssues(projectId).stream()
                        .map(IssueDto::from)
                        .toList()
        );
    }

    @GetMapping("/{projectId}/{issueId}")
    public ResponseEntity<Issue> getIssueById(
            @PathVariable Long projectId,
            @PathVariable Long issueId) {
        return ResponseEntity.ok(issueService.getProjectIssueById(projectId, issueId));
    }

    @GetMapping("/{projectId}/title/{title}")
    public ResponseEntity<Issue> getIssueByTitle(
            @PathVariable Long projectId,
            @PathVariable String title) {
        return ResponseEntity.ok(issueService.getProjectIssueByTitle(projectId, title));
    }

    @GetMapping("/{projectId}/severity/{severity}")
    public ResponseEntity<List<Issue>> getIssuesBySeverity(
            @PathVariable Long projectId,
            @PathVariable Severity severity) {
        return ResponseEntity.ok(issueService.getProjectIssuesBySeverity(projectId, severity));
    }

    @GetMapping("/{projectId}/overdue")
    public ResponseEntity<List<Issue>> getOverdueIssues(@PathVariable Long projectId) {
        return ResponseEntity.ok(issueService.getOverdueProjectIssues(projectId));
    }

    @PostMapping("/from-event-group/{groupId}")
    public ResponseEntity<IssueDto> createFromEventGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        return ResponseEntity.ok(IssueDto.from(issueService.createFromEventGroup(groupId, authentication)));
    }
}
