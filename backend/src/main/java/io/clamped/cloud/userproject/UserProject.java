package io.clamped.cloud.userproject;

import io.clamped.cloud.project.Project;
import io.clamped.cloud.user.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProject {
    @EmbeddedId
    private UserProjectId id = new UserProjectId();


    // MapsId takes the ID from the student and the ID from the vulnerability, and combine them to create the primary key for this record.
    // This primary key is also called the composite key in this context (merges the two primary/foreign keys)
    // Foreign keys live in the combining table
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId") // create foreign key
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    // @MapsId will generate an embedded ID, no need for sequence generation or instantiation in constructor
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("projectId") // boilerplate, references the UserVulnerabilityId composite create
    @JoinColumn(name = "project_id")
    @JsonIgnore
    private Project project;

    @Enumerated(EnumType.STRING)
    private ProjectRole role; // ADMIN, PROGRAMMER, VIEWER

    // Only for seeding (without id)
    public UserProject(User user, Project project, ProjectRole role) {
        this.user = user;
        this.project = project;
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserProject)) return false;
        UserProject other = (UserProject) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
