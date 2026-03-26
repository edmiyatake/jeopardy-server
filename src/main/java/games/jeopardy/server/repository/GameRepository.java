package games.jeopardy.server.repository;

import games.jeopardy.server.entity.Game;
import games.jeopardy.server.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link Game} sessions.
 *
 * Most lobby and gameplay operations route through this repository. Beyond the
 * inherited CRUD methods, two patterns are worth noting:
 *      - Room-code lookups: Players join via a short human-readable code.
 *       {@link #findByRoomCode} and {@link #existsByRoomCode} serve the join and
 *       uniqueness-check flows respectively.
 *     - Eager-fetch variants: {@link #findByRoomCodeWithPlayers} and
 *       {@link #findByIdWithQuestions} use {@code JOIN FETCH} to pull related
 *       collections in a single SQL query. Prefer these over the plain finders
 *       whenever you know you'll need the associations immediately, to avoid
 *       Hibernate issuing N+1 select statements for each lazy collection.
 *
 */
@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

    /**
     * Looks up a game by its lobby room code.
     * This is the primary join path — players enter the room code on the front end
     * and the server calls this method to resolve the session.
     *
     * @param roomCode the short alphanumeric code displayed in the lobby (e.g. "XKCD42")
     * @return the matching game, or {@link Optional#empty()} if no active game has that code
     */
    Optional<Game> findByRoomCode(String roomCode);

    /**
     * Returns {@code true} if a room code is already in use.
     * Call this during room creation to retry generation if there is a collision,
     * rather than catching a unique-constraint violation from the database.
     *
     * @param roomCode the candidate room code to check
     */
    boolean existsByRoomCode(String roomCode);

    /**
     * Returns all games currently in a given lifecycle state.
     * Useful for admin dashboards (e.g. list all {@code IN_PROGRESS} games)
     * and for cleanup jobs (e.g. find stale {@code WAITING} rooms to expire).
     *
     * @param status the {@link GameStatus} to filter by
     */
    List<Game> findByStatus(GameStatus status);

    /**
     * Returns all games a player has participated in, newest first.
     * Used for the player history / profile screen. Note the join goes through
     * {@code game_players} — this finds any game the player joined, regardless
     * of whether they finished it.
     *
     * @param playerId the player whose game history to retrieve
     */
    @Query("""
            SELECT g FROM Game g
            JOIN g.gamePlayers gp
            WHERE gp.player.id = :playerId
            ORDER BY g.createdAt DESC
            """)
    List<Game> findByPlayerId(@Param("playerId") UUID playerId);

    /**
     * Loads a game <em>and</em> its player roster in a single database round trip,
     * using {@code JOIN FETCH} to avoid lazy-loading N+1 issues.
     *
     * <p>Use this variant — rather than {@link #findByRoomCode} — whenever you need
     * to render the lobby screen or build the live scoreboard, where all player
     * records are required immediately.
     *
     * @param roomCode the room code to look up
     * @return the game with its {@code gamePlayers} and nested {@code player} records
     *         fully populated, or {@link Optional#empty()} if not found
     */
    @Query("""
            SELECT g FROM Game g
            JOIN FETCH g.gamePlayers gp
            JOIN FETCH gp.player
            WHERE g.roomCode = :roomCode
            """)
    Optional<Game> findByRoomCodeWithPlayers(@Param("roomCode") String roomCode);

    /**
     * Loads a game <em>and</em> its full question board in a single database round trip.
     *
     * <p>Use this variant when building or rendering the Jeopardy board grid, where
     * all {@link games.jeopardy.server.entity.GameQuestion} records (including the
     * nested {@link games.jeopardy.server.entity.Question} clue text) are needed at once.
     *
     * @param gameId the game ID to look up
     * @return the game with its {@code gameQuestions} and nested {@code question} records
     *         fully populated, or {@link Optional#empty()} if not found
     */
    @Query("""
            SELECT g FROM Game g
            JOIN FETCH g.gameQuestions gq
            JOIN FETCH gq.question
            WHERE g.id = :gameId
            """)
    Optional<Game> findByIdWithQuestions(@Param("gameId") UUID gameId);
}