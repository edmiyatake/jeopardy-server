package games.jeopardy.server.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Join table record linking a {@link Player} to a {@link Game}, with per-session state.
 *
 * This entity models the many-to-many relationship between players and games while
 * carrying game-specific data that doesn't belong on either side alone — most importantly
 * the player's score within this game, which is separate from their lifetime
 * {@link Player#getTotalScore()}.
 *
 * Composite key: The primary key is {@code (game_id, player_id)} via
 * {@link GamePlayerId}. Using {@code @EmbeddedId} rather than a surrogate key keeps
 * the schema aligned with the underlying join table and prevents duplicate rows at
 * the database level.
 *
 * Score updates: Prefer
 * {@link games.jeopardy.server.repository.GamePlayerRepository#adjustScore} for atomic
 * score deltas (correct/incorrect answers) rather than reading the entity, mutating it,
 * and saving — the repository method issues a single {@code UPDATE} and avoids
 * optimistic-lock conflicts under concurrent buzzer races.
 *
 * Active flag: {@code isActive = false} marks a player who disconnected or
 * was removed from the game. Inactive players are excluded from the live leaderboard
 * but their score history is retained for the final results screen.
 */
@Entity
@Table(name = "game_players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamePlayer {

    /**
     * Composite primary key composed of {@code game_id} and {@code player_id}.
     * Both UUIDs must be set before persisting.
     */
    @EmbeddedId
    private GamePlayerId id;

    /**
     * The game session this record belongs to. Mapped by the {@code game_id}
     * portion of the composite key.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId")
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * The player participating in this game. Mapped by the {@code player_id}
     * portion of the composite key.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("playerId")
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /**
     * Points earned (or lost) by this player within this game session only.
     * Updated after every answered clue. Do <em>not</em> use this to derive
     * the global leaderboard — query {@link Player#getTotalScore()} for that.
     */
    @Column(name = "score")
    @Builder.Default
    private Long score = 0L;

    /**
     * Whether the player is still connected and participating. Set to
     * {@code false} on disconnect or voluntary exit. Inactive players are
     * hidden from the live scoreboard but their data is preserved.
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 1-based seat position assigned when the player joins the lobby.
     * Used to determine turn order and display order on the front end.
     */
    @Column(name = "player_order")
    private Integer playerOrder;

    /** Stamped when the player successfully joins the lobby. */
    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;

    // -----------------------------------------------------------------
    // Composite key
    // -----------------------------------------------------------------

    /**
     * Embeddable composite key for the {@code game_players} join table.
     *
     * Must implement {@link Serializable} and provide value-based
     * {@code equals}/{@code hashCode} (provided by Lombok's {@code @EqualsAndHashCode})
     * so JPA can correctly manage entity identity and caching.
     */
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class GamePlayerId implements Serializable {

        /** FK to {@link Game#getId()}. */
        @Column(name = "game_id")
        private UUID gameId;

        /** FK to {@link Player#getId()}. */
        @Column(name = "player_id")
        private UUID playerId;
    }
}