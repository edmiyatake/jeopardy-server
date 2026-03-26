package games.jeopardy.server.repository;

import games.jeopardy.server.entity.Question;
import games.jeopardy.server.enums.QuestionDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Data access layer for the shared {@link Question} bank.
 *
 * Questions are authored once and reused across many game sessions. This repository
 * provides the queries needed to browse the bank and, most importantly, to build a
 * game board by selecting the right clues before a session starts.
 *
 * Board-building flow (typical call sequence):
 * - Call {@link #findDistinctCategoryBy()} to get all available categories.
 * - Pick 6 categories (randomly or by player vote).
 * - Call {@link #findByCategoriesOrderedForBoard(List)} to fetch all 30 clues
 *       (6 categories × 5 difficulty tiers) in a single query.
 * - Create {@link games.jeopardy.server.entity.GameQuestion} join records from
 *       the result and persist them to the game.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    /**
     * Returns all questions in a given category, unsorted.
     * Useful for admin UIs that browse the question bank by topic.
     *
     * @param category the exact category string to match
     */
    List<Question> findByCategory(String category);

    /**
     * Returns all questions at a specific difficulty tier, across all categories.
     * Useful for generating themed rounds or difficulty-filtered practice modes.
     *
     * @param difficulty the {@link QuestionDifficulty} tier to filter by
     */
    List<Question> findByDifficulty(QuestionDifficulty difficulty);

    /**
     * Returns questions matching both a specific category and difficulty tier.
     * In a well-seeded database this should return exactly one clue per
     * {@code (category, difficulty)} pair — the building block of a board cell.
     *
     * @param category   the category column header
     * @param difficulty the difficulty tier (board row)
     */
    List<Question> findByCategoryAndDifficulty(String category, QuestionDifficulty difficulty);

    /**
     * Returns the distinct list of category strings available in the question bank.
     * Used by the board-building service to present category choices to the host
     * before a game starts.
     */
    List<String> findDistinctCategoryBy();

    /**
     * Fetches all questions for the given categories, sorted by category name then
     * point value ascending — the natural reading order of a Jeopardy board
     * (left-to-right columns, top-to-bottom rows).
     *
     * <p>Use this single query instead of N per-category queries to avoid
     * round-trip overhead during board setup.
     *
     * @param categories the 6 (or fewer) category names chosen for this game's board
     */
    @Query("""
            SELECT q FROM Question q
            WHERE q.category IN :categories
            ORDER BY q.category, q.pointValue
            """)
    List<Question> findByCategoriesOrderedForBoard(@Param("categories") List<String> categories);

    /**
     * Selects a random sample of questions from a single category.
     * Useful for generating practice rounds or Daily Double candidates.
     *
     * <p>Uses a native PostgreSQL {@code RANDOM()} call — not portable to other
     * databases without modification. The {@code LIMIT} clause is applied in SQL
     * rather than in Java to avoid loading the entire category into memory.
     *
     * @param category the category to sample from
     * @param limit    the maximum number of questions to return
     */
    @Query(value = """
            SELECT * FROM questions
            WHERE category = :category
            ORDER BY RANDOM()
            LIMIT :limit
            """, nativeQuery = true)
    List<Question> findRandomByCategory(@Param("category") String category,
                                        @Param("limit") int limit);
}