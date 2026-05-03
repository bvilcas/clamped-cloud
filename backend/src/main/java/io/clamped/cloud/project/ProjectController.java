package io.clamped.cloud.project;

import io.clamped.cloud.backendconfig.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(path = "api/v1/projects")

public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService =  projectService;
    }

    // ✅ Create Project
    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createProject(@Valid @RequestBody ProjectCreationRequest request, Authentication authentication) {
        Project saved = projectService.createProject(request, authentication);
        return ResponseEntity.ok(new ApiResponse(true, "Project created", saved));
    }

    @PreAuthorize("@projectSecurity.hasProjectRole(authentication, #id, 'LEAD')")
    // ✅ Update Project
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectUpdateRequest request,
            Authentication authentication
    ) {
        Project updated = projectService.updateProject(id, request, authentication);
        return ResponseEntity.ok(new ApiResponse(true, "Project updated", updated));
    }

    @PreAuthorize("@projectSecurity.hasProjectRole(authentication, #id, 'LEAD')")
    // ✅ Delete Project
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteProject(@PathVariable Long id, Authentication authentication) {
        System.out.println("Deleting project with id " + id);
        projectService.deleteProject(id, authentication);
        return ResponseEntity.ok(new ApiResponse(true, "Project deleted"));
    }
}
