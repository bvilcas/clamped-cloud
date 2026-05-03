package io.clamped.cloud.admin;

import io.clamped.cloud.issue.Issue;
import io.clamped.cloud.issue.IssueRepository;
import io.clamped.cloud.issue.IssueStatus;
import io.clamped.cloud.issue.Severity;
import io.clamped.cloud.project.Project;
import io.clamped.cloud.project.ProjectRepository;
import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AdminService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;

    public AdminService(PasswordEncoder passwordEncoder, UserRepository userRepository,
                        IssueRepository issueRepository,
                        ProjectRepository projectRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User with email '" + email + "' not found"));
    }

    @Transactional
    public void updateUserFirstOrLastName(Long userId, String firstName, String lastName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        boolean updated = false;

        if (firstName != null && !firstName.isBlank()) {
            user.setFirstname(firstName);
            updated = true;
        }

        if (lastName != null && !lastName.isBlank()) {
            user.setLastname(lastName);
            updated = true;
        }

        if (!updated) {
            throw new IllegalArgumentException("At least one non-blank field (firstName or lastName) must be provided.");
        }

        userRepository.save(user);
    }

    @Transactional
    public User updateUserEmailOrPassword(Long userId, String email, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        boolean updated = false;

        if (email != null && !email.isBlank()) {
            user.setEmail(email);
            updated = true;
        }

        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
            updated = true;
        }

        if (!updated) {
            throw new IllegalArgumentException("At least one non-blank field (email or password) must be provided.");
        }

        user.setCredentialsChangedAt(Instant.now());
        userRepository.save(user);
        return user;
    }

    // ===========================================Admin Issue Stuff=============================================
    @Transactional(readOnly = true)
    public List<Issue> getIssues() {
        List<Issue> issues = issueRepository.findAll();
        if (issues.isEmpty()) throw new EntityNotFoundException("No issues found");
        return issues;
    }

    public Issue getIssueById(Long issueId) {
        return issueRepository.findIssueById(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Issue with id '" + issueId + "' not found"));
    }

    public Issue getIssueByTitle(String title) {
        return issueRepository.findIssueByTitle(title)
                .orElseThrow(() -> new EntityNotFoundException("Issue with title '" + title + "' not found"));
    }

    public List<Issue> getIssuesBySeverity(Severity severity) {
        List<Issue> issues = issueRepository.findIssuesBySeverity(severity);
        if (issues.isEmpty()) throw new EntityNotFoundException("Issues with severity " + severity + " not found");
        return issues;
    }

    public List<Issue> getOverdueIssues() {
        List<Issue> issues = issueRepository.findByDueAtBeforeAndStatusNot(Instant.now(), IssueStatus.VERIFIED);
        if (issues.isEmpty()) throw new EntityNotFoundException("No overdue issues found");
        return issues;
    }

    // =============================================== Project Functions ====================================
    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with ID " + id));
    }

    @Transactional(readOnly = true)
    public List<Project> getProjectsByName(String name) {
        List<Project> results = projectRepository.findByName(name);
        if (results.isEmpty()) throw new EntityNotFoundException("No projects found with name: " + name);
        return results;
    }
}
