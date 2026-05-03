package io.clamped.cloud;

import io.clamped.cloud.project.Project;
import io.clamped.cloud.project.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
class ProjectRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void findByName_returnsProjects() {
        Project project = new Project("Test Project", "Description", Instant.now(), null);
        entityManager.persistAndFlush(project);

        List<Project> found = projectRepository.findByName("Test Project");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Test Project");
    }
}
