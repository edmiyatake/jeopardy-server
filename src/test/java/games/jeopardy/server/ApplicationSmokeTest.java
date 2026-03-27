package games.jeopardy.server;

import games.jeopardy.server.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Smoke test — the single most important test in Phase 2.
 *
 * Verifies three things in one shot:
 * - The full Spring application context starts without errors.
 * - Flyway migrations run successfully and produce the expected tables.
 * - All five repositories are wired into the context and queryable.
 *
 * Uses {@code @SpringBootTest} (full context) rather than {@code @DataJpaTest}
 * (slice) so we catch wiring issues across the whole application, not just the
 * persistence layer. If this test fails, nothing else will pass — fix it first.
 */
@SpringBootTest
@Testcontainers
class ApplicationSmokeTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("jeopardy_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired PlayerRepository playerRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired GameRepository gameRepository;
    @Autowired GamePlayerRepository gamePlayerRepository;
    @Autowired GameQuestionRepository gameQuestionRepository;
    @Autowired DataSource dataSource;

    @Test
    void contextLoads() {
        // If this test method runs at all, the context started successfully.
        // All @Autowired fields above will have thrown if any bean was missing.
        assertThat(playerRepository).isNotNull();
        assertThat(questionRepository).isNotNull();
        assertThat(gameRepository).isNotNull();
        assertThat(gamePlayerRepository).isNotNull();
        assertThat(gameQuestionRepository).isNotNull();
    }

    @Test
    void flywayCreatesAllExpectedTables() throws Exception {
        // Query the Postgres information_schema to confirm every table exists.
        // This catches typos in @Table(name="...") before any CRUD test runs.
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(
                     null, "public", null, new String[]{"TABLE"})) {

            java.util.List<String> tables = new java.util.ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME").toLowerCase());
            }

            assertThat(tables).contains(
                    "players",
                    "questions",
                    "games",
                    "game_players",
                    "game_questions"
            );
        }
    }

    @Test
    void allRepositoriesReturnEmptyOnFreshDatabase() {
        // Sanity check: a brand-new schema has no rows.
        // Also confirms each repository can execute a SELECT without errors.
        assertThatNoException().isThrownBy(() -> {
            assertThat(playerRepository.count()).isZero();
            assertThat(questionRepository.count()).isZero();
            assertThat(gameRepository.count()).isZero();
            assertThat(gamePlayerRepository.count()).isZero();
            assertThat(gameQuestionRepository.count()).isZero();
        });
    }
}