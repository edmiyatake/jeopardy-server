package games.jeopardy.server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameQuestion {

    @EmbeddedId
    private GameQuestionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId")
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "is_answered")
    @Builder.Default
    private Boolean isAnswered = false;

    @Column(name = "answered_by_player_id")
    private UUID answeredByPlayerId;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "was_correct")
    private Boolean wasCorrect;

    @Column(name = "round")
    @Builder.Default
    private Integer round = 1;

    @Column(name = "board_position")
    private Integer boardPosition;

    // -----------------------------------------------------------------
    // Composite key
    // -----------------------------------------------------------------

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class GameQuestionId implements Serializable {

        @Column(name = "game_id")
        private UUID gameId;

        @Column(name = "question_id")
        private UUID questionId;
    }
}