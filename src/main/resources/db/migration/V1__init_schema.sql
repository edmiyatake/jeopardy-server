-- V1__init_schema.sql
-- Core schema for the Jeopardy clone
-- Matches JPA entities in games.jeopardy.server.entity

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------
-- players
-- ---------------------------------------------------------------
CREATE TABLE players (
                         id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                         username       VARCHAR(50)  NOT NULL UNIQUE,
                         display_name   VARCHAR(100) NOT NULL,
                         email          VARCHAR(255) NOT NULL UNIQUE,
                         password_hash  VARCHAR(255) NOT NULL,
                         total_score    BIGINT       NOT NULL DEFAULT 0,
                         games_played   INT          NOT NULL DEFAULT 0,
                         games_won      INT          NOT NULL DEFAULT 0,
                         created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                         updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------
-- questions
-- ---------------------------------------------------------------
-- Difficulty stored as VARCHAR to match @Enumerated(EnumType.STRING)
-- Values: TWO_HUNDRED, FOUR_HUNDRED, SIX_HUNDRED, EIGHT_HUNDRED, ONE_THOUSAND
CREATE TABLE questions (
                           id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                           category     VARCHAR(100) NOT NULL,
                           clue         TEXT         NOT NULL,
                           answer       TEXT         NOT NULL,
                           difficulty   VARCHAR(20)  NOT NULL,
                           point_value  INT          NOT NULL,
                           air_date     DATE,
                           created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                           updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_questions_category   ON questions (category);
CREATE INDEX idx_questions_difficulty ON questions (difficulty);

-- ---------------------------------------------------------------
-- games
-- ---------------------------------------------------------------
-- Status stored as VARCHAR to match @Enumerated(EnumType.STRING)
-- Values: WAITING, IN_PROGRESS, FINISHED, ABANDONED
CREATE TABLE games (
                       id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       room_code      VARCHAR(10)  NOT NULL UNIQUE,
                       status         VARCHAR(20)  NOT NULL DEFAULT 'WAITING',
                       max_players    INT          NOT NULL DEFAULT 6,
                       current_round  INT          NOT NULL DEFAULT 1,
                       host_player_id UUID         REFERENCES players (id),
                       started_at     TIMESTAMPTZ,
                       finished_at    TIMESTAMPTZ,
                       created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_games_status    ON games (status);
CREATE INDEX idx_games_room_code ON games (room_code);

-- ---------------------------------------------------------------
-- game_players  (join table: who is in a game, their per-session score)
-- ---------------------------------------------------------------
CREATE TABLE game_players (
                              game_id      UUID    NOT NULL REFERENCES games (id)   ON DELETE CASCADE,
                              player_id    UUID    NOT NULL REFERENCES players (id) ON DELETE CASCADE,
                              score        BIGINT  NOT NULL DEFAULT 0,
                              is_active    BOOLEAN NOT NULL DEFAULT TRUE,
                              player_order INT,
                              joined_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              PRIMARY KEY (game_id, player_id)
);

CREATE INDEX idx_game_players_game   ON game_players (game_id);
CREATE INDEX idx_game_players_player ON game_players (player_id);

-- ---------------------------------------------------------------
-- game_questions  (which clues are on the board for a game)
-- ---------------------------------------------------------------
CREATE TABLE game_questions (
                                game_id               UUID    NOT NULL REFERENCES games (id)     ON DELETE CASCADE,
                                question_id           UUID    NOT NULL REFERENCES questions (id),
                                is_answered           BOOLEAN NOT NULL DEFAULT FALSE,
                                answered_by_player_id UUID    REFERENCES players (id),
                                answered_at           TIMESTAMPTZ,
                                was_correct           BOOLEAN,
                                round                 INT     NOT NULL DEFAULT 1,
                                board_position        INT,
                                PRIMARY KEY (game_id, question_id)
);

CREATE INDEX idx_game_questions_game ON game_questions (game_id);