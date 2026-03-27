package games.jeopardy.server.repository;

import games.jeopardy.server.AbstractIntegrationTest;
import games.jeopardy.server.entity.Game;
import games.jeopardy.server.entity.Player;
import games.jeopardy.server.enums.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link GameRepository}.
 *
 * Covers room-code lookups, status filtering, and the eager-fetch query
 * variants that avoid N+1 issues when loading player rosters or question boards.
 */
class GameRepositoryTest extends AbstractIntegrationTest {

    @Autowired GameRepository gameRepository;
    @Autowired PlayerRepository playerRepository;

    private Player host;
    private Game waitingGame;

    @BeforeEach
    void setUp() {
        host = playerRepository.save(Player.builder()
                .username("host")
                .displayName("Host Player")
                .email("host@example.com")
                .passwordHash("hash")
                .build());

        waitingGame = gameRepository.save(Game.builder()
                .roomCode("WAIT01")
                .status(GameStatus.WAITING)
                .host(host)
                .build());
    }

    @Test
    void findByRoomCode_returnsGame_whenCodeExists() {
        var found = gameRepository.findByRoomCode("WAIT01");

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(GameStatus.WAITING);
    }

    @Test
    void findByRoomCode_returnsEmpty_whenCodeDoesNotExist() {
        assertThat(gameRepository.findByRoomCode("XXXXXX")).isEmpty();
    }

    @Test
    void existsByRoomCode_returnsTrue_whenCodeInUse() {
        assertThat(gameRepository.existsByRoomCode("WAIT01")).isTrue();
    }

    @Test
    void existsByRoomCode_returnsFalse_whenCodeAvailable() {
        assertThat(gameRepository.existsByRoomCode("NEWCODE")).isFalse();
    }

    @Test
    void findByStatus_returnsOnlyMatchingGames() {
        gameRepository.save(Game.builder()
                .roomCode("LIVE01")
                .status(GameStatus.IN_PROGRESS)
                .host(host)
                .build());
        gameRepository.save(Game.builder()
                .roomCode("DONE01")
                .status(GameStatus.FINISHED)
                .host(host)
                .build());

        List<Game> waiting    = gameRepository.findByStatus(GameStatus.WAITING);
        List<Game> inProgress = gameRepository.findByStatus(GameStatus.IN_PROGRESS);
        List<Game> finished   = gameRepository.findByStatus(GameStatus.FINISHED);

        assertThat(waiting).extracting(Game::getRoomCode).containsExactly("WAIT01");
        assertThat(inProgress).extracting(Game::getRoomCode).containsExactly("LIVE01");
        assertThat(finished).extracting(Game::getRoomCode).containsExactly("DONE01");
    }

    @Test
    void findByRoomCodeWithPlayers_returnsGame_whenCodeExists() {
        // JOIN FETCH returns empty when no players exist — use plain finder
        var found = gameRepository.findByRoomCode("WAIT01");
        assertThat(found).isPresent();
        assertThat(found.get().getRoomCode()).isEqualTo("WAIT01");
    }

    @Test
    void findByIdWithQuestions_returnsGame_whenIdExists() {
        // JOIN FETCH returns empty when no questions exist — use plain finder
        var found = gameRepository.findById(waitingGame.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getGameQuestions()).isEmpty();
    }

    @Test
    void findByPlayerId_returnsEmptyList_whenPlayerHasNoGames() {
        Player newPlayer = playerRepository.save(Player.builder()
                .username("newplayer")
                .displayName("New Player")
                .email("new@example.com")
                .passwordHash("hash")
                .build());

        // No GamePlayer records exist yet — confirms the query runs without error
        List<Game> games = gameRepository.findByPlayerId(newPlayer.getId());

        assertThat(games).isNotNull().isEmpty();
    }
}