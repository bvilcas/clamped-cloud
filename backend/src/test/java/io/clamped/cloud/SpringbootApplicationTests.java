// ==================== 1. ClampedCloudApplicationTests.java ====================
package io.clamped.cloud;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class ClampedCloudApplicationTests {

	@Test
	void contextLoads() {
		// Verifies Spring context loads successfully
	}

	@Test
	void mainMethodRuns() {
		assertDoesNotThrow(() -> ClampedCloudApplication.main(new String[] {}));
	}
}
