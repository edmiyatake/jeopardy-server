-- V1__init_schema.sql
-- Core schema for the Jeopardy clone

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------
-- players
-- ---------------------------------------------------------------
CREATE TABLE players (
                         id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                         username    VARCHAR(50) NOT NULL UNIQUE,
                         email       VARCHAR(255) NOT NULL UNIQUE,
                         password    VARCHAR(255) NOT NULL,
                         created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------
-- questions
-- ---------------------------------------------------------------
CREATE TYPE question_difficulty AS ENUM ('SINGLE', 'DOUBLE', 'FINAL');

CREATE TABLE questions (
                           id          UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
                           category    VARCHAR(100)        NOT NULL,
                           value       INT                 NOT NULL CHECK (value IN (200, 400, 600, 800, 1000)),
                           clue        TEXT                NOT NULL,
                           answer      TEXT                NOT NULL,
                           difficulty  question_difficulty NOT NULL DEFAULT 'SINGLE',
                           created_at  TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_questions_category ON questions (category);
CREATE INDEX idx_questions_difficulty ON questions (difficulty);

-- ---------------------------------------------------------------
-- games
-- ---------------------------------------------------------------
CREATE TYPE game_status AS ENUM (
    'WAITING',
    'BOARD_SELECT',
    'CLUE_ACTIVE',
    'BUZZ_OPEN',
    'ANSWERING',
    'FINAL_JEOPARDY',
    'COMPLETE'
);

CREATE TABLE games (
                       id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                       status      game_status NOT NULL DEFAULT 'WAITING',
                       created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       ended_at    TIMESTAMPTZ
);

CREATE INDEX idx_games_status ON games (status);

-- ---------------------------------------------------------------
-- game_players  (join table: who is in a game, their score)
-- ---------------------------------------------------------------
CREATE TABLE game_players (
                              game_id     UUID    NOT NULL REFERENCES games (id) ON DELETE CASCADE,
                              player_id   UUID    NOT NULL REFERENCES players (id) ON DELETE CASCADE,
                              score       INT     NOT NULL DEFAULT 0,
                              final_wager INT,
                              is_host     BOOLEAN NOT NULL DEFAULT FALSE,
                              PRIMARY KEY (game_id, player_id)
);

CREATE INDEX idx_game_players_game ON game_players (game_id);
CREATE INDEX idx_game_players_player ON game_players (player_id);

-- ---------------------------------------------------------------
-- game_questions  (which clues are on the board for a game)
-- ---------------------------------------------------------------
CREATE TABLE game_questions (
                                game_id       UUID    NOT NULL REFERENCES games (id) ON DELETE CASCADE,
                                question_id   UUID    NOT NULL REFERENCES questions (id),
                                is_revealed   BOOLEAN NOT NULL DEFAULT FALSE,
                                answered_by   UUID    REFERENCES players (id),
                                PRIMARY KEY (game_id, question_id)
);

CREATE INDEX idx_game_questions_game ON game_questions (game_id);