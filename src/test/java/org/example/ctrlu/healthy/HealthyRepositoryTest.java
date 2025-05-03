package org.example.ctrlu.healthy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HealthyRepositoryTest {

    @Autowired
    private HealthyRepository healthyRepository;

    @Container
    static MySQLContainer mySQLContainer = new MySQLContainer("mysql:8")
            .withDatabaseName("foureach_test")
            .withUsername("root")
            .withPassword("");

    @DynamicPropertySource
    public static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", mySQLContainer::getDriverClassName);
    }

    @Test
    void saveAndFindHealthy() {
        // given
        Healthy healthy = Healthy.builder().name("test").build();

        // when
        healthyRepository.save(healthy);
        Healthy found = healthyRepository.findById(healthy.getId()).orElse(null);

        // then
        assertThat(found).isNotNull();
    }
}