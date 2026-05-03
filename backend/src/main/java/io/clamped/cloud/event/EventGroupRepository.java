package io.clamped.cloud.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventGroupRepository extends JpaRepository<EventGroup, Long> {
    Optional<EventGroup> findByProjectIdAndFingerprint(Long projectId, String fingerprint);

    List<EventGroup> findByProjectIdOrderByLastSeenDesc(Long projectId);

    List<EventGroup> findByProjectIdAndEnvironmentOrderByLastSeenDesc(Long projectId, EventEnvironment environment);
}
