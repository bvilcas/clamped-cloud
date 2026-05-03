package io.clamped.cloud.user;

import io.clamped.cloud.userissue.UserIssue;
import io.clamped.cloud.userproject.UserProject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data // lombak (generates everything needed)
// help me build my object in an easy way using a design pattern builder.
// more readable and flexible using .builder()
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
// avoid ambiguity between this user class and the one on postgres (which contains my log in info)
@Table(name = "_user")
public class User {
    @Id
    // Auto-Generation
    @SequenceGenerator(
            name = "user_sequence",         // Name used inside the Java app to refer to this generator
            sequenceName = "user_sequence", // Name of the actual database sequence object
            allocationSize = 1                 // Number of IDs to allocate at once (1 = no batching; get a new ID every time)
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,  // Use a database sequence to generate the ID
            generator = "user_sequence"       // Link this ID generator to the one defined above
    )
    private Long id;
    private String firstname;
    private String lastname;
    @Column(nullable = false, unique = true)
    private String email; // Everyone must have a unique email
    // already implemented because of lombak, I implemented anyway
    private String password;
    @Enumerated(EnumType.STRING) // Fixed set of constants. Will show as strings on the database and not # (ordinal)
    private Role role; // user can only have one role because the field "role" is just one enum value, not a list

    // "Hey, I’m User.java, and I know I’m related to many UserIssue entries via the user field in that class."
    @OneToMany(mappedBy = "user", orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private Set<UserIssue> userIssues = new HashSet<>();

    // User ↔ Project
    @OneToMany(mappedBy = "user", orphanRemoval = true)
    private Set<UserProject> userProjects = new HashSet<>();

    // Timestamp that is in UTC (perfect for comparing across systems and with JWTs whose iat, exp work with UTC)
    @Column(name = "password_changed_at")
    @Nullable // can be null
    private Instant credentialsChangedAt; // to prevent old access tokens from being used in other sessions

    // Whether to receive email alerts for in-app notifications
    @Column(name = "email_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean emailNotificationsEnabled = false;

    // we need these cuz we are using email as "username"
    public String getUsername() {
        return email;
    }

    public void setUsername(String username) {
        this.email = username;
    }

    // ✅ If you prefer explicit equals/hashCode instead of Lombok:
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
