package games.jeopardy.server.repository;

import games.jeopardy.server.AbstractIntegrationTest;
import games.jeopardy.server.entity.Question;
import games.jeopardy.server.enums.QuestionDifficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link QuestionRepository}.
 *
 * Covers category and difficulty filtering, the board-builder query, and
 * the random-sampling native query used for Daily Double selection.
 */
class QuestionRepositoryTest extends AbstractIntegrationTest {

    @Autowired QuestionRepository questionRepository;

    @BeforeEach
    void setUp() {
        // Seed a base SCIENCE $200 clue present in every test
        questionRepository.save(Question.builder()
                .category("SCIENCE")
                .clue("This is the powerhouse of the cell")
                .answer("What is the mitochondria?")
                .difficulty(QuestionDifficulty.TWO_HUNDRED)
                .pointValue(200)
                .build());
    }

    @Test
    void findByCategory_returnsOnlyMatchingCategory() {
        questionRepository.save(Question.builder()
                .category("HISTORY")
                .clue("First US president")
                .answer("Who is George Washington?")
                .difficulty(QuestionDifficulty.TWO_HUNDRED)
                .pointValue(200)
                .build());

        List<Question> results = questionRepository.findByCategory("SCIENCE");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getClue()).contains("powerhouse");
    }

    @Test
    void findByCategory_returnsEmpty_whenCategoryDoesNotExist() {
        assertThat(questionRepository.findByCategory("SPORTS")).isEmpty();
    }

    @Test
    void findByDifficulty_returnsOnlyMatchingTier() {
        // V2 seeds 6 TWO_HUNDRED questions (one per category), plus the 1 we insert
        List<Question> twoHundred = questionRepository.findByDifficulty(QuestionDifficulty.TWO_HUNDRED);
        List<Question> oneThousand = questionRepository.findByDifficulty(QuestionDifficulty.ONE_THOUSAND);

        assertThat(twoHundred.size()).isGreaterThanOrEqualTo(1);
        assertThat(twoHundred).anyMatch(q -> q.getClue().equals("This is the powerhouse of the cell"));
        assertThat(oneThousand.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void findByCategoryAndDifficulty_returnsExactMatch() {
        questionRepository.save(Question.builder()
                .category("SCIENCE")
                .clue("Speed of light in a vacuum")
                .answer("What is 299,792,458 m/s?")
                .difficulty(QuestionDifficulty.FOUR_HUNDRED)
                .pointValue(400)
                .build());

        var result = questionRepository.findByCategoryAndDifficulty(
                "SCIENCE", QuestionDifficulty.TWO_HUNDRED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPointValue()).isEqualTo(200);
    }
    @Test
    void findDistinctCategories_returnsUniqueCategories() {
        // V2 seeds 6 categories — confirm our inserted ones appear in the results
        questionRepository.save(Question.builder()
                .category("UNIQUE_TEST_CATEGORY")
                .clue("Test clue")
                .answer("Test answer")
                .difficulty(QuestionDifficulty.FOUR_HUNDRED)
                .pointValue(400)
                .build());

        List<String> categories = questionRepository.findDistinctCategories();

        assertThat(categories).contains("UNIQUE_TEST_CATEGORY");
        assertThat(categories).doesNotHaveDuplicates();
    }

    @Test
    void findByCategoriesOrderedForBoard_returnsSortedByCategory_thenPointValue() {
        questionRepository.save(Question.builder()
                .category("HISTORY")
                .clue("First US president")
                .answer("Who is George Washington?")
                .difficulty(QuestionDifficulty.TWO_HUNDRED)
                .pointValue(200)
                .build());
        questionRepository.save(Question.builder()
                .category("HISTORY")
                .clue("Year of the French Revolution")
                .answer("What is 1789?")
                .difficulty(QuestionDifficulty.FOUR_HUNDRED)
                .pointValue(400)
                .build());
        questionRepository.save(Question.builder()
                .category("SCIENCE")
                .clue("Speed of light")
                .answer("What is 299,792,458 m/s?")
                .difficulty(QuestionDifficulty.FOUR_HUNDRED)
                .pointValue(400)
                .build());

        List<Question> board = questionRepository
                .findByCategoriesOrderedForBoard(List.of("HISTORY", "SCIENCE"));

        // Expected order: HISTORY $200, HISTORY $400, SCIENCE $200, SCIENCE $400
        assertThat(board).hasSize(4);
        assertThat(board.get(0).getCategory()).isEqualTo("HISTORY");
        assertThat(board.get(0).getPointValue()).isEqualTo(200);
        assertThat(board.get(1).getCategory()).isEqualTo("HISTORY");
        assertThat(board.get(1).getPointValue()).isEqualTo(400);
        assertThat(board.get(2).getCategory()).isEqualTo("SCIENCE");
        assertThat(board.get(2).getPointValue()).isEqualTo(200);
        assertThat(board.get(3).getCategory()).isEqualTo("SCIENCE");
        assertThat(board.get(3).getPointValue()).isEqualTo(400);
    }

    @Test
    void findByCategoriesOrderedForBoard_excludesUnrequestedCategories() {
        questionRepository.save(Question.builder()
                .category("HISTORY")
                .clue("A history clue")
                .answer("Who is Napoleon?")
                .difficulty(QuestionDifficulty.TWO_HUNDRED)
                .pointValue(200)
                .build());

        // Only request SCIENCE — HISTORY should not appear
        List<Question> board = questionRepository
                .findByCategoriesOrderedForBoard(List.of("SCIENCE"));

        assertThat(board).allMatch(q -> q.getCategory().equals("SCIENCE"));
    }

    @Test
    void findRandomByCategory_returnsAtMostRequestedLimit() {
        // Seed 3 SCIENCE questions total (1 from setUp + 2 here)
        questionRepository.save(Question.builder()
                .category("SCIENCE")
                .clue("Second science clue")
                .answer("What is gravity?")
                .difficulty(QuestionDifficulty.FOUR_HUNDRED)
                .pointValue(400)
                .build());
        questionRepository.save(Question.builder()
                .category("SCIENCE")
                .clue("Third science clue")
                .answer("What is a neutron?")
                .difficulty(QuestionDifficulty.SIX_HUNDRED)
                .pointValue(600)
                .build());

        List<Question> sample = questionRepository.findRandomByCategory("SCIENCE", 2);

        assertThat(sample).hasSize(2);
        assertThat(sample).allMatch(q -> q.getCategory().equals("SCIENCE"));
    }
}