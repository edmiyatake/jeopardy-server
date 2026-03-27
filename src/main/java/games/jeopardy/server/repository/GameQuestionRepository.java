package games.jeopardy.server.repository;

import games.jeopardy.server.entity.GameQuestion;
import games.jeopardy.server.entity.GameQuestion.GameQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link GameQuestion} — the per-game board state records.
 *
 * Each row tracks one clue's position and answered status on a specific game's board.
 * The two most common runtime operations are:
 *   - Board rendering:{@link #findByGameIdAndRound} fetches the 30 clues
 *       for the current round so the front end can draw the board grid and grey out
 *       already-answered cells.
 *   - Marking answers:</b> {@link #markAnswered} atomically records the outcome
 *       of a player's response — who answered, when, and whether they were correct —
 *       in a single {@code UPDATE} to avoid race conditions during concurrent buzzer
 *       submissions.
 *
 * Round completion detection:Call {@link #countUnansweredInRound} after
 * each answer to check whether all clues in the current round have been resolved.
 * When it returns {@code 0}, the game service should advance {@code currentRound}
 * on the {@link games.jeopardy.server.entity.Game} entity (or transition to
 * {@code FINISHED} if the last round just ended).
 */
@Repository
public interface GameQuestionRepository extends JpaRepository<GameQuestion, GameQuestionId> {

    /**
     * Returns all board clues for a game, across all rounds.
     * Prefer {@link #findByGameIdAndRound} during active gameplay to avoid loading
     * clues from future rounds prematurely.
     *
     * @param gameId the game session to query
     */
    List<GameQuestion> findByGameId(UUID gameId);

    /**
     * Returns the clues for a specific round of a game.
     * The primary query for building the board grid on the front end.
     * Round {@code 1} = Jeopardy, {@code 2} = Double Jeopardy, {@code 3} = Final.
     *
     * @param gameId the game session
     * @param round  the round number (1-based)
     */
    List<GameQuestion> findByGameIdAndRound(UUID gameId, int round);

    /**
     * Filters board clues by their answered state.
     * Pass {@code false} to get clues still available for selection,
     * {@code true} to get clues already resolved (useful for audit/replay views).
     *
     * @param gameId     the game session
     * @param isAnswered {@code false} for available clues, {@code true} for resolved ones
     */
    List<GameQuestion> findByGameIdAndIsAnswered(UUID gameId, boolean isAnswered);

    /**
     * Looks up a specific clue placement on a game's board.
     * Called when a player selects a cell to fetch the clue text before
     * broadcasting it to all players over WebSocket.
     *
     * @param gameId     the game session
     * @param questionId the question (clue) the player selected
     */
    Optional<GameQuestion> findByGameIdAndQuestionId(UUID gameId, UUID questionId);

    /**
     * Counts the clues still awaiting a response in a given round.
     * Use this after each answer to detect round completion: when this returns
     * {@code 0}, all clues in the round have been resolved and the game service
     * should advance to the next round or end the game.
     *
     * @param gameId the game session
     * @param round  the round to check
     * @return the number of unanswered clues remaining in that round
     */
    @Query("""
            SELECT COUNT(gq) FROM GameQuestion gq
            WHERE gq.game.id = :gameId
              AND gq.round = :round
              AND gq.isAnswered = false
            """)
    int countUnansweredInRound(@Param("gameId") UUID gameId, @Param("round") int round);

    /**
     * Atomically records the outcome of a player's answer attempt in a single
     * {@code UPDATE} — no read step required.
     *
     * <p>Sets {@code isAnswered = true}, {@code answeredByPlayerId}, {@code answeredAt},
     * and {@code wasCorrect} in one statement. Prefer this over loading the entity,
     * mutating fields, and calling {@code save()} to avoid stale-read conflicts during
     * concurrent buzzer submissions on the same clue.
     *
     * <p>Must be called inside a transaction. Annotate the calling service method
     * with {@code @Transactional}. Pair with
     * {@link GamePlayerRepository#adjustScore} in the same transaction to keep
     * board state and scores consistent.
     *
     * @param gameId     the game session
     * @param questionId the clue being resolved
     * @param playerId   the player who gave the final response
     * @param answeredAt the timestamp of the response (use {@link Instant#now()})
     * @param wasCorrect {@code true} if the response matched the expected answer
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE GameQuestion gq
            SET gq.isAnswered          = true,
                gq.answeredByPlayerId  = :playerId,
                gq.answeredAt          = :answeredAt,
                gq.wasCorrect          = :wasCorrect
            WHERE gq.game.id     = :gameId
              AND gq.question.id = :questionId
            """)
    void markAnswered(@Param("gameId") UUID gameId,
                      @Param("questionId") UUID questionId,
                      @Param("playerId") UUID playerId,
                      @Param("answeredAt") Instant answeredAt,
                      @Param("wasCorrect") boolean wasCorrect);
}