package io.clamped.cloud.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findTop100ByProjectIdOrderBySentAtAsc(Long projectId);
}
