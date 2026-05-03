package io.clamped.cloud.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    // or you can do @Query("SELECT s FROM Student s WHERE s.email = ?1")

    boolean existsByEmail(String email);

    List<User> findTop10ByEmailContainingIgnoreCase(String email);
}
