package io.clamped.cloud.backendconfig;

import io.clamped.cloud.issue.Issue;
import io.clamped.cloud.issue.IssueRepository;
import io.clamped.cloud.issue.IssueStatus;
import io.clamped.cloud.issue.IssueType;
import io.clamped.cloud.issue.Severity;
import io.clamped.cloud.jwtconfig.JwtService;
import io.clamped.cloud.project.Project;
import io.clamped.cloud.project.ProjectRepository;
import io.clamped.cloud.userissue.RoleInIssue;
import io.clamped.cloud.userissue.UserIssue;
import io.clamped.cloud.userissue.UserIssueId;
import io.clamped.cloud.userissue.UserIssueRepository;
import io.clamped.cloud.userproject.ProjectRole;
import io.clamped.cloud.userproject.UserProject;
import io.clamped.cloud.userproject.UserProjectRepository;
import io.clamped.cloud.user.Role;
import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner init(UserRepository userRepository, IssueRepository issueRepository,
                           ProjectRepository projectRepository, JwtService jwtService,
                           UserIssueRepository userIssueRepository, PasswordEncoder passwordEncoder,
                           UserProjectRepository userProjectRepository) {
        return args -> {

            // === USERS ===
            User user1 = new User();
            user1.setEmail("alex@gmail.com");
            user1.setPassword(passwordEncoder.encode("123"));
            user1.setFirstname("Alex");
            user1.setLastname("Carter");
            user1.setRole(Role.USER);
            userRepository.save(user1);

            User brent = new User();
            brent.setEmail("bvilcas@gmail.com");
            brent.setPassword(passwordEncoder.encode("123"));
            brent.setFirstname("Brent");
            brent.setLastname("Vilcas");
            brent.setRole(Role.USER);
            userRepository.save(brent);

            User user3 = new User();
            user3.setEmail("jordan@gmail.com");
            user3.setPassword(passwordEncoder.encode("123"));
            user3.setFirstname("Jordan");
            user3.setLastname("Reed");
            user3.setRole(Role.USER);
            userRepository.save(user3);

            User admin = new User();
            admin.setEmail("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode("123"));
            admin.setFirstname("System");
            admin.setLastname("Admin");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            // === PROJECTS ===
            Project project1 = new Project(
                    "Authentication Project",
                    "This is the starter project for testing.",
                    Instant.now(),
                    Instant.now()
            );
            project1.setApiKey(UUID.randomUUID().toString());
            projectRepository.save(project1);

            Project project2 = new Project(
                    "Document Security Project",
                    "This contains document access control testing.",
                    Instant.now(),
                    Instant.now()
            );
            project2.setApiKey(UUID.randomUUID().toString());
            projectRepository.save(project2);

            // === LINK USER ↔ PROJECT ===
            UserProject link = new UserProject();
            link.setUser(user1);
            link.setProject(project1);
            link.setRole(ProjectRole.LEAD);
            userProjectRepository.save(link);

            UserProject link1 = new UserProject();
            link1.setUser(brent);
            link1.setProject(project1);
            link1.setRole(ProjectRole.PROGRAMMER);
            userProjectRepository.save(link1);

            UserProject link2 = new UserProject();
            link2.setUser(user3);
            link2.setProject(project2);
            link2.setRole(ProjectRole.LEAD);
            userProjectRepository.save(link2);

            // === ISSUES ===
            Issue i1 = new Issue(
                    "Authentication bypass in MFA",
                    "Attackers can brute-force 2FA codes due to missing retry limits.",
                    IssueType.SECURITY,
                    "CVE-2025-4101",
                    "CWE-307",
                    Severity.HIGH,
                    IssueStatus.REPORTED,
                    null,
                    Instant.now().minusSeconds(86400),
                    Instant.parse("2025-11-25T17:00:00Z"),
                    null,
                    null,
                    "github.com/brentcodes/auth-service",
                    "c88fa91bdd0e4f9e9afb77d502aaf922cbb994d2",
                    project1
            );

            Issue i2 = new Issue(
                    "SQL Injection in search query",
                    "Unsanitized input appended to SQL statement allows data exfiltration.",
                    IssueType.SECURITY,
                    "CVE-2025-5533",
                    "CWE-89",
                    Severity.CRITICAL,
                    IssueStatus.IN_PROGRESS,
                    null,
                    Instant.now().minusSeconds(172800),
                    null,
                    null,
                    null,
                    "github.com/brentcodes/search-service",
                    "91eeaf2230fc4cb3b21ef1299b7c11cb865fbb90",
                    project1
            );

            Issue i3 = new Issue(
                    "Broken access control in document preview",
                    "Users can view other teams' files by guessing preview IDs.",
                    IssueType.SECURITY,
                    null,
                    "CWE-639",
                    Severity.HIGH,
                    IssueStatus.REPORTED,
                    null,
                    Instant.now(),
                    Instant.parse("2025-11-30T23:59:59Z"),
                    null,
                    null,
                    "github.com/brentcodes/document-service",
                    "2ab44aa9de991122e34f3d12bb123e88ad6711ac",
                    project2
            );

            Issue i4 = new Issue(
                    "Insecure object reference",
                    "Users can access restricted files by guessing object IDs.",
                    IssueType.SECURITY,
                    null,
                    "CWE-639",
                    Severity.HIGH,
                    IssueStatus.REPORTED,
                    null,
                    Instant.now(),
                    Instant.parse("2025-11-30T23:59:59Z"),
                    null,
                    null,
                    "github.com/brentcodes/document-service",
                    "2ab44aa9de991122e34f3d12bb123e88ad6711ac",
                    project1
            );

            issueRepository.save(i1);
            issueRepository.save(i2);
            issueRepository.save(i3);
            issueRepository.save(i4);

            // === USER ISSUE LINKS ===
            UserIssue ui1 = UserIssue.builder()
                    .id(new UserIssueId(user1.getId(), i1.getId()))
                    .user(user1)
                    .issue(i1)
                    .role(RoleInIssue.REPORTER)
                    .selfAssigned(false)
                    .assignedAt(Instant.now().minusSeconds(86400))
                    .build();

            UserIssue ui2 = UserIssue.builder()
                    .id(new UserIssueId(brent.getId(), i2.getId()))
                    .user(brent)
                    .issue(i2)
                    .role(RoleInIssue.ASSIGNEE)
                    .selfAssigned(true)
                    .assignedAt(Instant.now())
                    .build();

            UserIssue ui3 = UserIssue.builder()
                    .id(new UserIssueId(user3.getId(), i3.getId()))
                    .user(user3)
                    .issue(i3)
                    .role(RoleInIssue.VERIFIER)
                    .selfAssigned(false)
                    .assignedAt(Instant.now())
                    .build();

            userIssueRepository.save(ui1);
            userIssueRepository.save(ui2);
            userIssueRepository.save(ui3);

            System.out.println("✅✅✅ Ready for Import ✅✅✅");
        };
    }
}
