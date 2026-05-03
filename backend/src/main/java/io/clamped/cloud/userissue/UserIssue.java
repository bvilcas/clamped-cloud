package io.clamped.cloud.userissue;

import io.clamped.cloud.issue.Issue;
import io.clamped.cloud.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_issue")
public class UserIssue {

    @EmbeddedId
    @Builder.Default
    private UserIssueId id = new UserIssueId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("issueId")
    @JoinColumn(name = "issue_id")
    @JsonIgnore
    private Issue issue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleInIssue role;

    @Column(nullable = false)
    private Instant assignedAt;

    private boolean selfAssigned;

    private Instant completedAt;

    // Seeding constructor (no id)
    public UserIssue(User user, Issue issue, RoleInIssue role,
                     Instant assignedAt, boolean selfAssigned, Instant completedAt) {
        this.user = user;
        this.issue = issue;
        this.role = role;
        this.assignedAt = assignedAt;
        this.selfAssigned = selfAssigned;
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserIssue other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
