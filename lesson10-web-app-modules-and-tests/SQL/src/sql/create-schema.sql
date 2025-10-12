
CREATE EXTENSION IF NOT EXISTS citext;

-- ===========================
-- Tipos de domínio (ENUMs)
-- ===========================
CREATE TYPE dice_face AS ENUM ('ACE','KING','QUEEN','JACK','TEN','NINE');

-- Força das mãos (ordem decrescente) conforme enunciado.
CREATE TYPE hand_rank AS ENUM (
  'FIVE_OF_A_KIND',
  'FOUR_OF_A_KIND',
  'FULL_HOUSE',
  'STRAIGHT',
  'THREE_OF_A_KIND',
  'TWO_PAIR',
  'ONE_PAIR',
  'BUST'
);

CREATE TYPE lobby_state AS ENUM ('OPEN','FULL','STARTED','CLOSED');
CREATE TYPE lobby_player_status AS ENUM ('WAITING','LEFT','KICKED');
CREATE TYPE match_state AS ENUM ('IN_PROGRESS','COMPLETED','CANCELLED');
CREATE TYPE tx_type AS ENUM ('ANTE','WIN','ADJUSTMENT');

-- ===========================
-- Utilizadores, convites e sessões
-- ===========================
CREATE TABLE app_user (
                          id                BIGSERIAL PRIMARY KEY,
                          username          CITEXT NOT NULL UNIQUE,
                          display_name      TEXT,
                          password_hash     TEXT NOT NULL,
                          balance_coins     INTEGER NOT NULL DEFAULT 0, -- saldo atual (inteiros)
                          created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Registo por convite (one-time use)
CREATE TABLE invitation (
                            code              TEXT PRIMARY KEY,  -- ex.: token curto/URL-safe
                            created_by        BIGINT NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
                            created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                            used_by           BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
                            used_at           TIMESTAMPTZ,
                            CONSTRAINT one_time_use CHECK ((used_by IS NULL) = (used_at IS NULL))
);

-- Autenticação por token (header ou cookie é detalhe da app)
CREATE TABLE auth_token (
                            id                BIGSERIAL PRIMARY KEY,
                            user_id           BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                            token             TEXT NOT NULL UNIQUE,      -- guarda hash do token se quiseres
                            issued_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
                            expires_at        TIMESTAMPTZ,
                            revoked_at        TIMESTAMPTZ
);

CREATE INDEX ix_auth_token_user ON auth_token(user_id);

-- ===========================
-- Lobbies
-- ===========================
CREATE TABLE lobby (
                       id                BIGSERIAL PRIMARY KEY,
                       lobby_Host         BIGINT NOT NULL REFERENCES app_user(id) ON DELETE RESTRICT,
                       name              TEXT NOT NULL,
                       description       TEXT,
                       min_players       INTEGER NOT NULL,
                       max_players       INTEGER NOT NULL,
                       rounds            INTEGER NOT NULL,
    --ante              INTEGER NOT NULL DEFAULT 1,  -- ante por ronda
                       state             lobby_state NOT NULL DEFAULT 'OPEN',
                       CONSTRAINT ck_players_bounds CHECK (min_players >= 2 AND max_players >= min_players),
                       CONSTRAINT ck_rounds_positive CHECK (rounds > 0),
                       CONSTRAINT ck_ante_positive CHECK (ante_coins > 0)
);


CREATE INDEX ix_lobby_state ON lobby(state);
CREATE INDEX ix_lobby_player_user ON lobby_player(user_id);

-- ===========================
-- Partidas (Matches)
-- ===========================
/*
CREATE TABLE match (
                       id                BIGSERIAL PRIMARY KEY,
                       lobby_id          BIGINT REFERENCES lobby(id) ON DELETE SET NULL,
                       state             match_state NOT NULL DEFAULT 'IN_PROGRESS',
                       started_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                       ended_at          TIMESTAMPTZ,
                       starting_player_user_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL
);

-- Participantes na partida (com ordem/seat p/ rotação de turnos)
CREATE TABLE match_player (
                              match_id          BIGINT NOT NULL REFERENCES match(id) ON DELETE CASCADE,
                              user_id           BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                              seat_no           INTEGER NOT NULL,               -- define ordem dos turnos
                              balance_start     INTEGER NOT NULL,               -- snapshot à entrada
                              balance_end       INTEGER,                        -- snapshot à saída/fim
                              PRIMARY KEY (match_id, user_id),
                              UNIQUE (match_id, seat_no)
);

CREATE INDEX ix_match_state ON match(state);
*/
-- ===========================
-- Rondas e Turnos
-- ===========================
CREATE TABLE round (
    --id                BIGSERIAL PRIMARY KEY,
    --match_id          BIGINT NOT NULL REFERENCES match(id) ON DELETE CASCADE,
                       round_no          BIGSERIAL PRIMARY KEY,               -- 1..number_of_rounds
                       state             round_state NOT NULL DEFAULT 'IN_PROGRESS',
                       pot               INTEGER NOT NULL,               -- copia do valor do lobby/match
                       results           List<Text> NOT NULL,               -- TODO: List of text ?
);

CREATE INDEX ix_round_match ON round(match_id);

-- Cada jogador joga 1 turno por ronda; até 3 lançamentos

-- Histórico dos lançamentos dentro de um turno (1..3)
CREATE TABLE Dice (
    -- TODO: completar
);

-- ===========================
-- Movimentos/Transações (saldo)
-- ===========================
CREATE TABLE wallet_tx (
                           id                BIGSERIAL PRIMARY KEY,
                           user_id           BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                           match_id          BIGINT REFERENCES match(id) ON DELETE SET NULL,
                           round_id          BIGINT REFERENCES round(id) ON DELETE SET NULL,
                           type              tx_type NOT NULL,               -- ANTE (-), WIN (+), ADJUSTMENT (+/-)
                           amount_coins      INTEGER NOT NULL,               -- débito < 0, crédito > 0
                           created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                           CHECK (amount_coins <> 0)
);

CREATE INDEX ix_wallet_user ON wallet_tx(user_id);
CREATE INDEX ix_wallet_round ON wallet_tx(round_id);
