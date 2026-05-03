package io.clamped.cloud.project;

import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserPrincipal;
import io.clamped.cloud.user.UserRepository;
import io.clamped.cloud.userproject.ProjectRole;
import io.clamped.cloud.userproject.UserProject;
import io.clamped.cloud.userproject.UserProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserProjectRepository userProjectRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository,  UserProjectRepository userProjectRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userProjectRepository = userProjectRepository;
    }

    // ✅ Create Project
    public Project createProject(ProjectCreationRequest request, Authentication authentication) {
        // 1. Get the authenticated user's ID (Runs JWT authentication filter)
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        // 2. Check user exists
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        Project project = new Project(request.name(), request.description(), Instant.now(), null);
        UserProject userProject = new UserProject(user, project, ProjectRole.LEAD); // Make the creator a lead automatically
        projectRepository.save(project);
        userProjectRepository.save(userProject);
        return project;
    }

    // ✅ Update Project
    public Project updateProject(Long id, ProjectUpdateRequest request, Authentication authentication) {
        // 1. Get the authenticated user's ID (Runs JWT authentication filter)
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        // 2. Check user exists
        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with ID " + id));

        // ✅ Only update if the field was provided
        if (request.name() != null && !request.name().isBlank()) {
            project.setName(request.name());
        }

        if (request.description() != null && !request.description().isBlank()) {
            project.setDescription(request.description());
        }

        project.setUpdatedAt(Instant.now());

        return projectRepository.save(project);
    }

    // ✅ Delete Project
    public void deleteProject(Long id, Authentication authentication) {
        // 1. Get the authenticated user's ID (Runs JWT authentication filter)
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        // 2. Check user exists
        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with ID " + id));


        System.out.println("User and projects checks are done");

        // ✅ Vulnerabilities (and their UserVulnerability links) WILL cascade because collection is maintained
        projectRepository.delete(project);
    }
}
