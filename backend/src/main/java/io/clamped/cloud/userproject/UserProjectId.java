package io.clamped.cloud.userproject;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class UserProjectId implements Serializable {
    private Long userId;
    private Long projectId;

    public UserProjectId() {
    }

    public UserProjectId(Long userId, Long projectId) { // used for creating relationships
        this.userId = userId;
        this.projectId = projectId;
    }
    // equals, hashCode
}
