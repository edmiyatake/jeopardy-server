package games.jeopardy.server;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests that require a real PostgreSQL database.
 *
 * Testcontainers starts a single PostgreSQL container for the entire test suite
 * (static field = one container shared across all subclasses). Flyway migrations
 * run automatically on startup, so the schema is always in sync with production.
 *
 * Any test class that needs database access should extend this class rather
 * than configuring Testcontainers individually — this keeps container startup to
 * one occurrence per test run.
 *
 * {@code @AutoConfigureTestDatabase(replace = NONE)} tells Spring Boot not to
 * substitute an in-memory H2 database, ensuring we test against real Postgres
 * behaviour (UUID generation, enum string storage, RANDOM() ordering, etc.).
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("jeopardy_test")
                .withUsername("test")
                .withPassword("test");
        POSTGRES.start(); // start once, shared across all test classes
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}