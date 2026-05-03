package io.clamped.cloud.event;

import io.clamped.cloud.issue.Issue;
import io.clamped.cloud.issue.Severity;
import io.clamped.cloud.project.Project;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_group",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "fingerprint"}))
public class EventGroup {

    @Id
    @SequenceGenerator(name = "event_group_sequence", sequenceName = "event_group_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_group_sequence")
    private Long id;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Severity severity = Severity.MEDIUM;

    @Builder.Default
    private Long count = 1L;

    private Instant firstSeen;
    private Instant lastSeen;

    @Builder.Default
    private boolean muted = false;

    private String tags;

    @Column(length = 500)
    private String exceptionClass;

    @Column(columnDefinition = "TEXT")
    private String stacktrace;

    @Column(length = 500)
    private String sourceFile;

    private Integer sourceLine;

    @Column(length = 255)
    private String sourceMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_issue_id")
    @JsonIgnore
    private Issue linkedIssue;
}
