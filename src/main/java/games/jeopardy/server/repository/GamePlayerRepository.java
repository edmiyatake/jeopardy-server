package games.jeopardy.server.repository;

import games.jeopardy.server.entity.GamePlayer;
import games.jeopardy.server.entity.GamePlayer.GamePlayerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link GamePlayer} — the per-session player state records.
 *
 * <p>The two most performance-sensitive operations in a live game both live here:
 * <ul>
 *   <li><b>Leaderboard:</b> {@link #findByGameIdOrderByScoreDesc} returns players
 *       ranked by score for the scoreboard overlay shown between rounds.</li>
 *   <li><b>Score adjustment:</b> {@link #adjustScore} issues a single atomic
 *       {@code UPDATE ... SET score = score + :delta} rather than a read-modify-write
 *       cycle, which prevents lost updates when two players buzz in simultaneously
 *       and their score changes are processed in close succession.</li>
 * </ul>
 */
@Repository
public interface GamePlayerRepository extends JpaRepository<GamePlayer, GamePlayerId> {

    /**
     * Returns all player-state records for a game session.
     * Useful when you need raw {@link GamePlayer} data for all participants
     * without needing the nested {@link games.jeopardy.server.entity.Player} details.
     *
     * @param gameId the game session to query
     */
    List<GamePlayer> findByGameId(UUID gameId);

    /**
     * Returns all game-session records for a single player.
     * Used to reconstruct a player's match history from the join-table side
     * (as opposed to {@link GameRepository#findByPlayerId}, which returns
     * the full {@link games.jeopardy.server.entity.Game} objects).
     *
     * @param playerId the player whose participation records to retrieve
     */
    List<GamePlayer> findByPlayerId(UUID playerId);

    /**
     * Returns the session record for one specific player in one specific game.
     * The primary access pattern during gameplay — called on every answer
     * submission to read the player's current score before adjusting it.
     *
     * @param gameId   the game session
     * @param playerId the player within that session
     */
    Optional<GamePlayer> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    /**
     * Returns {@code true} if the player is already enrolled in the game.
     * Call this in the join-lobby flow to prevent duplicate entries without
     * loading the full {@link GamePlayer} object.
     *
     * @param gameId   the game session to check
     * @param playerId the player to check for
     */
    boolean existsByGameIdAndPlayerId(UUID gameId, UUID playerId);

    /**
     * Counts the number of players who are still active (connected) in a game.
     * Used to determine whether to pause the game when a player disconnects,
     * or to check minimum-player requirements before starting.
     *
     * @param gameId the game session to count active players in
     */
    @Query("""
            SELECT COUNT(gp) FROM GamePlayer gp
            WHERE gp.game.id = :gameId AND gp.isActive = true
            """)
    int countActivePlayers(@Param("gameId") UUID gameId);

    /**
     * Returns the player roster for a game, sorted by score descending.
     * The primary query for building the live scoreboard and the end-of-game
     * results screen. The first element in the returned list is the current leader.
     *
     * @param gameId the game session to rank
     */
    @Query("""
            SELECT gp FROM GamePlayer gp
            WHERE gp.game.id = :gameId
            ORDER BY gp.score DESC
            """)
    List<GamePlayer> findByGameIdOrderByScoreDesc(@Param("gameId") UUID gameId);

    /**
     * Atomically adds {@code delta} to a player's score in a single {@code UPDATE}.
     *
     * <p>Pass a positive {@code delta} for a correct answer, negative for incorrect.
     * This avoids the read-modify-write pattern ({@code findById → setScore → save}),
     * which is susceptible to lost updates when concurrent answer submissions arrive
     * within the same transaction window.
     *
     * <p>Must be called inside a transaction. Annotate the calling service method
     * with {@code @Transactional}.
     *
     * @param gameId   the game session
     * @param playerId the player whose score to adjust
     * @param delta    points to add (positive) or subtract (negative)
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE GamePlayer gp
            SET gp.score = gp.score + :delta
            WHERE gp.game.id = :gameId AND gp.player.id = :playerId
            """)
    void adjustScore(@Param("gameId") UUID gameId,
                     @Param("playerId") UUID playerId,
                     @Param("delta") long delta);
}