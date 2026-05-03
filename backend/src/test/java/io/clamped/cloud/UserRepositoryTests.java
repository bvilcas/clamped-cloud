package io.clamped.cloud;

import io.clamped.cloud.user.Role;
import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
class UserRepositoryTests {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Test
	void findByEmail_returnsUser() {
		User user = User.builder()
				.firstname("John")
				.lastname("Doe")
				.email("john@test.com")
				.password("pass123")
				.role(Role.USER)
				.build();
		entityManager.persistAndFlush(user);

		var found = userRepository.findByEmail("john@test.com");

		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo("john@test.com");
	}

	@Test
	void existsByEmail_returnsTrue() {
		User user = User.builder()
				.firstname("Jane")
				.lastname("Doe")
				.email("jane@test.com")
				.password("pass123")
				.role(Role.USER)
				.build();
		entityManager.persistAndFlush(user);

		assertThat(userRepository.existsByEmail("jane@test.com")).isTrue();
		assertThat(userRepository.existsByEmail("fake@test.com")).isFalse();
	}
}
