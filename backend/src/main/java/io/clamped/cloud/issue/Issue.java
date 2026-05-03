package io.clamped.cloud.issue;

import io.clamped.cloud.project.Project;
import io.clamped.cloud.userissue.UserIssue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(name = "issue")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Issue {

    @Id
    @SequenceGenerator(name = "issue_sequence", sequenceName = "issue_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "issue_sequence")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IssueType type = IssueType.OTHER;

    // Security context (optional — relevant when type = SECURITY)
    private String cveId;
    private String cweId;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private IssueStatus status;

    private Instant updatedAt;
    private Instant reportedAt;
    private Instant dueAt;
    private Instant patchedAt;
    private Instant verifiedAt;

    private String repository;
    private String commitHash;

    // Evidence reference — opaque pointer to local machine storage, never fetched by cloud
    private UUID evidenceMachineId;
    private UUID evidenceLocalId;
    @Column(length = 500)
    private String evidenceNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<UserIssue> userIssues = new HashSet<>();

    // Seeding constructor (no id)
    public Issue(String title, String description, IssueType type, String cveId, String cweId,
                 Severity severity, IssueStatus status, Instant updatedAt, Instant reportedAt,
                 Instant dueAt, Instant patchedAt, Instant verifiedAt,
                 String repository, String commitHash, Project project) {
        this.title = title;
        this.description = description;
        this.type = type != null ? type : IssueType.OTHER;
        this.cveId = cveId;
        this.cweId = cweId;
        this.severity = severity;
        this.status = status;
        this.updatedAt = updatedAt;
        this.reportedAt = reportedAt;
        this.dueAt = dueAt;
        this.patchedAt = patchedAt;
        this.verifiedAt = verifiedAt;
        this.repository = repository;
        this.commitHash = commitHash;
        this.project = project;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Issue other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
