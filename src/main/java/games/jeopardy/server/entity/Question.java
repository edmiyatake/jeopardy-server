package games.jeopardy.server.entity;

import games.jeopardy.server.enums.QuestionDifficulty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single Jeopardy clue stored in the question bank.
 *
 * {@code Question} records are reusable across multiple games. They are never
 * modified during gameplay — the game board is built by selecting a subset of
 * questions and linking them to a {@link Game} via {@link GameQuestion} join records.
 *
 * Board layout: A standard Jeopardy round uses 6 categories × 5 difficulty
 * tiers = 30 clues. The service layer queries for one {@code Question} per
 * {@code (category, difficulty)} pair when setting up the board.
 *
 * Scoring: {@code pointValue} drives score adjustments during the game.
 * A correct response adds {@code pointValue} to the answering player's score;
 * an incorrect response subtracts it. The {@link QuestionDifficulty} enum provides
 * a semantic label, but {@code pointValue} is the authoritative number used at
 * runtime (allowing Double Jeopardy rounds to double the stakes without altering
 * the enum or the clue record itself).
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    /**
     * Surrogate primary key. UUID generation is delegated to the database so the
     * key is available immediately after persistence.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The board column header (e.g. "SCIENCE", "POP CULTURE").
     * Multiple questions share the same category string — it is not a foreign key.
     */
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    /**
     * The clue text read aloud to players (e.g. "This planet is the largest in our solar system").
     * Stored as TEXT to accommodate long clues without truncation.
     */
    @Column(name = "clue", nullable = false, columnDefinition = "TEXT")
    private String clue;

    /**
     * The expected response, traditionally phrased as a question (e.g. "What is Jupiter?").
     * Answer-checking logic in the service layer should do case-insensitive, punctuation-tolerant
     * matching rather than an exact string comparison.
     */
    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    /**
     * Semantic difficulty tier. Stored as the enum name (e.g. {@code "SIX_HUNDRED"})
     * for readability. Used when building the board to ensure each category has
     * exactly one clue per tier.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private QuestionDifficulty difficulty;

    /**
     * The number of points awarded or deducted for this clue.
     * Typically matches the difficulty tier (200, 400, 600, 800, 1000) but may
     * be doubled for a Double Jeopardy round without changing the {@code difficulty} label.
     */
    @Column(name = "point_value", nullable = false)
    private Integer pointValue;

    /**
     * The original broadcast date if this clue was sourced from an archival dataset.
     * Nullable — internally authored questions may not have an air date.
     */
    @Column(name = "air_date")
    private java.time.LocalDate airDate;

    /** Set once on insert; questions in the bank are never re-dated. */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /** Updated whenever the clue or answer text is corrected. */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * All game-board placements for this question. A single question can appear
     * on multiple game boards (across different sessions) — each placement is
     * tracked independently by a {@link GameQuestion} record.
     */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GameQuestion> gameQuestions = new ArrayList<>();
}