package games.jeopardy.server.repository;

import games.jeopardy.server.AbstractIntegrationTest;
import games.jeopardy.server.entity.Game;
import games.jeopardy.server.entity.GamePlayer;
import games.jeopardy.server.entity.GamePlayer.GamePlayerId;
import games.jeopardy.server.entity.Player;
import games.jeopardy.server.enums.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link GamePlayerRepository}.
 *
 * Covers per-session score management, active-player counting, leaderboard
 * ordering, and the atomic {@code adjustScore} modifying query.
 */
class GamePlayerRepositoryTest extends AbstractIntegrationTest {

    @Autowired PlayerRepository playerRepository;
    @Autowired GameRepository gameRepository;
    @Autowired GamePlayerRepository gamePlayerRepository;

    private Player player1;
    private Player player2;
    private Game game;

    @BeforeEach
    void setUp() {
        player1 = playerRepository.save(Player.builder()
                .username("alice")
                .displayName("Alice")
                .email("alice@example.com")
                .passwordHash("hash1")
                .build());

        player2 = playerRepository.save(Player.builder()
                .username("bob")
                .displayName("Bob")
                .email("bob@example.com")
                .passwordHash("hash2")
                .build());

        game = gameRepository.save(Game.builder()
                .roomCode("GPRTEST")
                .status(GameStatus.IN_PROGRESS)
                .host(player1)
                .build());
    }

    @Test
    void canPersistAndRetrieveGamePlayer() {
        GamePlayerId id = new GamePlayerId(game.getId(), player1.getId());
        gamePlayerRepository.save(GamePlayer.builder()
                .id(id).game(game).player(player1).playerOrder(1).build());

        var found = gamePlayerRepository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getScore()).isZero();
        assertThat(found.get().getIsActive()).isTrue();
        assertThat(found.get().getPlayerOrder()).isEqualTo(1);
    }

    @Test
    void findByGameId_returnsAllPlayersInGame() {
        persistGamePlayer(player1, 1, 0L);
        persistGamePlayer(player2, 2, 0L);

        List<GamePlayer> players = gamePlayerRepository.findByGameId(game.getId());

        assertThat(players).hasSize(2);
    }

    @Test
    void findByPlayerId_returnsAllGamesForPlayer() {
        persistGamePlayer(player1, 1, 0L);

        // Create a second game and enroll player1 there too
        Game game2 = gameRepository.save(Game.builder()
                .roomCode("GAME2")
                .status(GameStatus.WAITING)
                .host(player1)
                .build());
        gamePlayerRepository.save(GamePlayer.builder()
                .id(new GamePlayerId(game2.getId(), player1.getId()))
                .game(game2).player(player1).playerOrder(1).build());

        List<GamePlayer> records = gamePlayerRepository.findByPlayerId(player1.getId());

        assertThat(records).hasSize(2);
    }

    @Test
    void findByGameIdAndPlayerId_returnsCorrectRecord() {
        persistGamePlayer(player1, 1, 0L);
        persistGamePlayer(player2, 2, 0L);

        var found = gamePlayerRepository
                .findByGameIdAndPlayerId(game.getId(), player2.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getPlayerOrder()).isEqualTo(2);
    }

    @Test
    void existsByGameIdAndPlayerId_returnsTrueForEnrolledPlayer() {
        persistGamePlayer(player1, 1, 0L);

        assertThat(gamePlayerRepository
                .existsByGameIdAndPlayerId(game.getId(), player1.getId())).isTrue();
        assertThat(gamePlayerRepository
                .existsByGameIdAndPlayerId(game.getId(), player2.getId())).isFalse();
    }

    @Test
    void countActivePlayers_excludesInactivePlayers() {
        persistGamePlayer(player1, 1, 0L);
        // Enroll player2 but mark them inactive (disconnected)
        gamePlayerRepository.save(GamePlayer.builder()
                .id(new GamePlayerId(game.getId(), player2.getId()))
                .game(game).player(player2).playerOrder(2).isActive(false).build());

        int count = gamePlayerRepository.countActivePlayers(game.getId());

        assertThat(count).isEqualTo(1);
    }

    @Test
    @Transactional
    void adjustScore_addsPositiveDelta_forCorrectAnswer() {
        persistGamePlayer(player1, 1, 0L);

        gamePlayerRepository.adjustScore(game.getId(), player1.getId(), 200L);

        var found = gamePlayerRepository
                .findByGameIdAndPlayerId(game.getId(), player1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getScore()).isEqualTo(200L);
    }

    @Test
    @Transactional
    void adjustScore_subtractsNegativeDelta_forIncorrectAnswer() {
        persistGamePlayer(player1, 1, 400L);

        gamePlayerRepository.adjustScore(game.getId(), player1.getId(), -200L);

        var found = gamePlayerRepository
                .findByGameIdAndPlayerId(game.getId(), player1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getScore()).isEqualTo(200L);
    }

    @Test
    @Transactional
    void adjustScore_canResultInNegativeScore() {
        persistGamePlayer(player1, 1, 0L);

        gamePlayerRepository.adjustScore(game.getId(), player1.getId(), -400L);

        var found = gamePlayerRepository
                .findByGameIdAndPlayerId(game.getId(), player1.getId());
        assertThat(found.get().getScore()).isEqualTo(-400L);
    }

    @Test
    void findByGameIdOrderByScoreDesc_returnsLeaderboardOrder() {
        persistGamePlayer(player1, 1, 600L);
        persistGamePlayer(player2, 2, 1000L);

        List<GamePlayer> leaderboard = gamePlayerRepository
                .findByGameIdOrderByScoreDesc(game.getId());

        assertThat(leaderboard).hasSize(2);
        assertThat(leaderboard.get(0).getPlayer().getId()).isEqualTo(player2.getId());
        assertThat(leaderboard.get(0).getScore()).isEqualTo(1000L);
        assertThat(leaderboard.get(1).getPlayer().getId()).isEqualTo(player1.getId());
        assertThat(leaderboard.get(1).getScore()).isEqualTo(600L);
    }

    @Test
    void gamePlayerId_equalityIsValueBased() {
        GamePlayerId id1 = new GamePlayerId(game.getId(), player1.getId());
        GamePlayerId id2 = new GamePlayerId(game.getId(), player1.getId());
        GamePlayerId different = new GamePlayerId(game.getId(), player2.getId());

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        assertThat(id1).isNotEqualTo(different);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private void persistGamePlayer(Player player, int order, long initialScore) {
        gamePlayerRepository.save(GamePlayer.builder()
                .id(new GamePlayerId(game.getId(), player.getId()))
                .game(game).player(player)
                .playerOrder(order).score(initialScore)
                .build());
    }
}