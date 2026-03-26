package games.jeopardy.server.enums;

/**
 * Represents the lifecycle states of a Jeopardy game session.
 *
 * <p>Games follow a strict linear progression:
 * <pre>
 *   WAITING ──► IN_PROGRESS ──► FINISHED
 *                    │
 *                    └──────────► ABANDONED
 * </pre>
 *
 * <p>The {@code status} field on {@link games.jeopardy.server.entity.Game} is stored
 * as a string column (via {@code @Enumerated(EnumType.STRING)}) so database
 * values remain human-readable and survive enum reordering.
 */
public enum GameStatus {

    /**
     * The game room has been created and the host is waiting for players to join.
     * No questions have been dealt yet. Players may join or leave freely.
     */
    WAITING,

    /**
     * The game is actively being played. The board is visible, players are
     * selecting clues, and scores are being updated in real time via WebSocket.
     */
    IN_PROGRESS,

    /**
     * All rounds have been completed and a winner has been determined.
     * Final scores are frozen and available for display on the results screen.
     */
    FINISHED,

    /**
     * The game ended prematurely — e.g. the host disconnected, all players left,
     * or an admin closed the session. Scores are preserved for audit purposes
     * but no winner is declared.
     */
    ABANDONED
}