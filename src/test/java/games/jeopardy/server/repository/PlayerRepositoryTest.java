package games.jeopardy.server.repository;

import games.jeopardy.server.AbstractIntegrationTest;
import games.jeopardy.server.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PlayerRepository}.
 *
 * Covers the custom lookup and existence-check methods used during
 * player registration and authentication flows.
 */
class PlayerRepositoryTest extends AbstractIntegrationTest {

    @Autowired PlayerRepository playerRepository;

    private Player savedPlayer;

    @BeforeEach
    void setUp() {
        savedPlayer = playerRepository.save(Player.builder()
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .build());
    }

    @Test
    void findByUsername_returnsPlayer_whenUsernameExists() {
        var found = playerRepository.findByUsername("testuser");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByUsername_returnsEmpty_whenUsernameDoesNotExist() {
        var found = playerRepository.findByUsername("nobody");

        assertThat(found).isEmpty();
    }

    @Test
    void findByEmail_returnsPlayer_whenEmailExists() {
        var found = playerRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void findByEmail_returnsEmpty_whenEmailDoesNotExist() {
        var found = playerRepository.findByEmail("nobody@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByUsername_returnsTrue_whenUsernameTaken() {
        assertThat(playerRepository.existsByUsername("testuser")).isTrue();
    }

    @Test
    void existsByUsername_returnsFalse_whenUsernameAvailable() {
        assertThat(playerRepository.existsByUsername("available_name")).isFalse();
    }

    @Test
    void existsByEmail_returnsTrue_whenEmailRegistered() {
        assertThat(playerRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    void existsByEmail_returnsFalse_whenEmailNotRegistered() {
        assertThat(playerRepository.existsByEmail("new@example.com")).isFalse();
    }
}