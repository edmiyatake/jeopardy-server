package games.jeopardy.server.repository;

import games.jeopardy.server.AbstractIntegrationTest;
import games.jeopardy.server.entity.Game;
import games.jeopardy.server.entity.GameQuestion;
import games.jeopardy.server.entity.GameQuestion.GameQuestionId;
import games.jeopardy.server.entity.Player;
import games.jeopardy.server.entity.Question;
import games.jeopardy.server.enums.GameStatus;
import games.jeopardy.server.enums.QuestionDifficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link GameQuestionRepository}.
 *
 * Covers board state queries (answered/unanswered filtering, round counting)
 * and the atomic {@code markAnswered} modifying query that records the outcome
 * of a player's response.
 */
class GameQuestionRepositoryTest extends AbstractIntegrationTest {

    @Autowired PlayerRepository playerRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired GameRepository gameRepository;
    @Autowired GameQuestionRepository gameQuestionRepository;

    private Player player;
    private Question question1;
    private Question question2;
    private Game game;

    @BeforeEach
    void setUp() {
        player = playerRepository.save(Player.builder()
                .username("tester")
                .displayName("Tester")
                .email("tester@example.com")
                .passwordHash("hash")
                .build());

        question1 = questionRepository.save(Question.builder()
                .category("SCIENCE")
                .clue("Powerhouse of the cell")
                .answer("What is the mitochondria?")
                .difficulty(QuestionDifficulty.TWO_HUNDRED)
                .pointValue(200)
                .build());

        question2 = questionRepository.save(Question.builder()
                .category("SCIENCE")
                .clue("Force equals mass times this")
                .answer("What is acceleration?")
                .difficulty(QuestionDifficulty.FOUR_HUNDRED)
                .pointValue(400)
                .build());

        game = gameRepository.save(Game.builder()
                .roomCode("GQTEST")
                .status(GameStatus.IN_PROGRESS)
                .host(player)
                .build());
    }

    @Test
    void canPersistAndRetrieveGameQuestion() {
        GameQuestionId id = new GameQuestionId(game.getId(), question1.getId());
        gameQuestionRepository.save(GameQuestion.builder()
                .id(id).game(game).question(question1)
                .round(1).boardPosition(0).build());

        var found = gameQuestionRepository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getIsAnswered()).isFalse();
        assertThat(found.get().getWasCorrect()).isNull();
        assertThat(found.get().getBoardPosition()).isZero();
    }

    @Test
    void findByGameId_returnsAllCluesForGame() {
        persistGameQuestion(question1, false, 0);
        persistGameQuestion(question2, false, 1);

        List<GameQuestion> all = gameQuestionRepository.findByGameId(game.getId());

        assertThat(all).hasSize(2);
    }

    @Test
    void findByGameIdAndRound_returnsOnlyCluesInThatRound() {
        persistGameQuestion(question1, false, 0);  // round 1 (default)

        // Persist question2 in round 2
        GameQuestionId id2 = new GameQuestionId(game.getId(), question2.getId());
        gameQuestionRepository.save(GameQuestion.builder()
                .id(id2).game(game).question(question2)
                .round(2).boardPosition(0).build());

        assertThat(gameQuestionRepository.findByGameIdAndRound(game.getId(), 1)).hasSize(1);
        assertThat(gameQuestionRepository.findByGameIdAndRound(game.getId(), 2)).hasSize(1);
    }

    @Test
    void findByGameIdAndIsAnswered_filtersCorrectly() {
        persistGameQuestion(question1, false, 0);
        persistGameQuestion(question2, true, 1);

        List<GameQuestion> unanswered = gameQuestionRepository
                .findByGameIdAndIsAnswered(game.getId(), false);
        List<GameQuestion> answered = gameQuestionRepository
                .findByGameIdAndIsAnswered(game.getId(), true);

        assertThat(unanswered).hasSize(1);
        assertThat(unanswered.get(0).getQuestion().getId()).isEqualTo(question1.getId());
        assertThat(answered).hasSize(1);
        assertThat(answered.get(0).getQuestion().getId()).isEqualTo(question2.getId());
    }

    @Test
    void findByGameIdAndQuestionId_returnsCorrectRecord() {
        persistGameQuestion(question1, false, 0);
        persistGameQuestion(question2, false, 1);

        var found = gameQuestionRepository
                .findByGameIdAndQuestionId(game.getId(), question2.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getBoardPosition()).isEqualTo(1);
    }

    @Test
    void countUnansweredInRound_countsOnlyUnanswered() {
        persistGameQuestion(question1, false, 0);
        persistGameQuestion(question2, true, 1);

        int count = gameQuestionRepository.countUnansweredInRound(game.getId(), 1);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void countUnansweredInRound_returnsZero_whenAllCluesAnswered() {
        persistGameQuestion(question1, true, 0);
        persistGameQuestion(question2, true, 1);

        int count = gameQuestionRepository.countUnansweredInRound(game.getId(), 1);

        // Service layer would advance to the next round at this point
        assertThat(count).isZero();
    }

    @Test
    @Transactional
    void markAnswered_setsAllFieldsCorrectly_forCorrectResponse() {
        persistGameQuestion(question1, false, 0);
        Instant answeredAt = Instant.now();

        gameQuestionRepository.markAnswered(
                game.getId(), question1.getId(), player.getId(), answeredAt, true);

        var found = gameQuestionRepository
                .findByGameIdAndQuestionId(game.getId(), question1.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getIsAnswered()).isTrue();
        assertThat(found.get().getAnsweredByPlayerId()).isEqualTo(player.getId());
        assertThat(found.get().getWasCorrect()).isTrue();
        assertThat(found.get().getAnsweredAt()).isNotNull();
    }

    @Test
    @Transactional
    void markAnswered_setsWasCorrectFalse_forIncorrectResponse() {
        persistGameQuestion(question1, false, 0);

        gameQuestionRepository.markAnswered(
                game.getId(), question1.getId(), player.getId(), Instant.now(), false);

        var found = gameQuestionRepository
                .findByGameIdAndQuestionId(game.getId(), question1.getId());

        assertThat(found.get().getWasCorrect()).isFalse();
        assertThat(found.get().getIsAnswered()).isTrue();
    }

    @Test
    void gameQuestionId_equalityIsValueBased() {
        GameQuestionId id1 = new GameQuestionId(game.getId(), question1.getId());
        GameQuestionId id2 = new GameQuestionId(game.getId(), question1.getId());
        GameQuestionId different = new GameQuestionId(game.getId(), question2.getId());

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        assertThat(id1).isNotEqualTo(different);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private void persistGameQuestion(Question question, boolean answered, int position) {
        GameQuestionId id = new GameQuestionId(game.getId(), question.getId());
        gameQuestionRepository.save(GameQuestion.builder()
                .id(id).game(game).question(question)
                .round(1).boardPosition(position).isAnswered(answered)
                .build());
    }
}