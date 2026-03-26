package games.jeopardy.server.enums;

/**
 * Represents the five point values (difficulty tiers) a Jeopardy clue can carry.
 * In classic Jeopardy, each category on the board contains one clue per dollar
 * value, from easiest ($200) to hardest ($1,000). This enum models that structure
 * and is used to:
 *   - Tag each {@link games.jeopardy.server.entity.Question} with its tier</li>
 *   - Filter questions when building a game board (one per tier per category)</li>
 *   - Drive score adjustments — a correct answer adds {@code pointValue},
 *       an incorrect answer subtracts it</li>
 * Stored as {@code EnumType.STRING} in the database so the column value
 * (e.g. {@code "TWO_HUNDRED"}) is self-documenting. The numeric point value
 * itself lives on the {@link games.jeopardy.server.entity.Question#pointValue}
 * field rather than being derived from the enum, which allows Double Jeopardy
 * rounds to double the stakes without changing the difficulty label.
 */
public enum QuestionDifficulty {

    /** Easiest tier — worth $200 in a standard round. */
    TWO_HUNDRED,

    /** Second tier — worth $400 in a standard round. */
    FOUR_HUNDRED,

    /** Middle tier — worth $600 in a standard round. */
    SIX_HUNDRED,

    /** Fourth tier — worth $800 in a standard round. */
    EIGHT_HUNDRED,

    /** Hardest tier — worth $1,000 in a standard round. */
    ONE_THOUSAND
}
