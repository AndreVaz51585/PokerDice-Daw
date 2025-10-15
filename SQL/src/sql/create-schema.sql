-- Create the schema dbo
CREATE SCHEMA IF NOT EXISTS dbo;
-- Ativar extensão para texto case-insensitive
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TYPE round_state AS ENUM ('IN_PROGRESS','COMPLETED');


-- Create table for users in the dbo schema
CREATE TABLE dbo.users
(
    id                  SERIAL PRIMARY KEY,
    name                VARCHAR(255)        NOT NULL,
    email               VARCHAR(255) UNIQUE NOT NULL,
    password_validation VARCHAR(255)        NOT NULL
);

create table dbo.Tokens
(
    token_validation VARCHAR(256) primary key,
    user_id          int references dbo.Users (id),
    created_at       bigint not null,
    last_used_at     bigint not null
);

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
-- TODO: Ver aula 13 para erceber como se faz o sql
-- TODO: Acho que tenho de pôr numa pasta fora à parte como está na aula 13 em que vou ter lá o SQL o Docker e tudo
-- TODO: Não preciso de memoria posso fazer tudo em SQL
-- TODO: Fazer testes para domain
CREATE TYPE lobby_state AS ENUM ('OPEN','FULL','STARTED','CLOSED');
CREATE TYPE lobby_player_status AS ENUM ('WAITING','LEFT','KICKED');
CREATE TYPE match_state AS ENUM ('IN_PROGRESS','COMPLETED','CANCELLED');
CREATE TYPE tx_type AS ENUM ('ANTE','WIN','ADJUSTMENT');

-- ===========================
-- Utilizadores, convites e sessões
-- ===========================
    --CREATE TABLE app_user (
      --            id                BIGSERIAL PRIMARY KEY,
        --          username          CITEXT NOT NULL UNIQUE,
          --        display_name      TEXT,
            --      password_hash     TEXT NOT NULL,
              --    balance_coins     INTEGER NOT NULL DEFAULT 0, -- saldo atual (inteiros)
                --  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
    --);

-- Registo por convite (one-time use)
CREATE TABLE invitation (
                            code              TEXT PRIMARY KEY,  -- ex.: token curto/URL-safe
                            created_by        BIGINT NOT NULL REFERENCES dbo.users(id) ON DELETE RESTRICT,
                            created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                            used_by           BIGINT REFERENCES dbo.users(id) ON DELETE SET NULL,
                            used_at           TIMESTAMPTZ,
                            CONSTRAINT one_time_use CHECK ((used_by IS NULL) = (used_at IS NULL))
);

-- Autenticação por token (header ou cookie é detalhe da app)
CREATE TABLE auth_token (
                            id                BIGSERIAL PRIMARY KEY,
                            user_id           BIGINT NOT NULL REFERENCES dbo.users(id) ON DELETE CASCADE,
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
                       lobby_Host         BIGINT NOT NULL REFERENCES dbo.users(id) ON DELETE RESTRICT,
                       name              TEXT NOT NULL,
                       description       TEXT,
                       min_players       INTEGER NOT NULL,
                       max_players       INTEGER NOT NULL,
                       rounds            INTEGER NOT NULL,
                       ante              INTEGER NOT NULL DEFAULT 1,  -- ante por ronda
                       state             lobby_state NOT NULL DEFAULT 'OPEN',
                       CONSTRAINT ck_players_bounds CHECK (min_players >= 2 AND max_players >= min_players),
                       CONSTRAINT ck_rounds_positive CHECK (rounds > 0),
                       CONSTRAINT ck_ante_positive CHECK (ante > 0)
);


CREATE INDEX ix_lobby_state ON lobby(state);
--CREATE INDEX ix_lobby_player_user ON lobby_player(user_id);


-- Lista de jogadores por lobby
CREATE TABLE IF NOT EXISTS lobby_player (
    lobby_id  BIGINT NOT NULL REFERENCES lobby(id)      ON DELETE CASCADE,
    user_id   BIGINT NOT NULL REFERENCES dbo.users(id)   ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (lobby_id, user_id)
);

CREATE INDEX IF NOT EXISTS ix_lobby_player_lobby ON lobby_player(lobby_id);
CREATE INDEX IF NOT EXISTS ix_lobby_player_user  ON lobby_player(user_id);

-- Trigger: ao criar um lobby, inserir automaticamente o host como player
CREATE OR REPLACE FUNCTION trg_lobby_add_host() RETURNS trigger AS $$
BEGIN
  INSERT INTO lobby_player (lobby_id, user_id)
  VALUES (NEW.id, NEW.lobby_host)
  ON CONFLICT DO NOTHING;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS lobby_add_host ON lobby;
CREATE TRIGGER lobby_add_host
AFTER INSERT ON lobby
FOR EACH ROW
EXECUTE FUNCTION trg_lobby_add_host();



-- ===========================
-- Partidas (Matches)
-- ===========================

CREATE TABLE match (
                       id                BIGSERIAL PRIMARY KEY,
                       lobby_id          BIGINT REFERENCES lobby(id) ON DELETE SET NULL,
                       state             match_state NOT NULL DEFAULT 'IN_PROGRESS',
                       started_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                       ended_at          TIMESTAMPTZ,
                       starting_player_user_id BIGINT REFERENCES dbo.users(id) ON DELETE SET NULL
);

-- Participantes na partida (com ordem/seat p/ rotação de turnos)
CREATE TABLE match_player (
                              match_id          BIGINT NOT NULL REFERENCES match(id) ON DELETE CASCADE,
                              user_id           BIGINT NOT NULL REFERENCES dbo.users(id) ON DELETE CASCADE,
                              seat_no           INTEGER NOT NULL,               -- define ordem dos turnos
                              balance_start     INTEGER NOT NULL,               -- snapshot à entrada
                              balance_end       INTEGER,                        -- snapshot à saída/fim
                              PRIMARY KEY (match_id, user_id),
                              UNIQUE (match_id, seat_no)
);

CREATE INDEX ix_match_state ON match(state);

-- ===========================
-- Rondas e Turnos
-- ===========================
CREATE TABLE round (
    --id                BIGSERIAL PRIMARY KEY,
    --match_id          BIGINT NOT NULL REFERENCES match(id) ON DELETE CASCADE,
                       round_no          BIGSERIAL PRIMARY KEY,               -- 1..number_of_rounds
                       state             round_state NOT NULL DEFAULT 'IN_PROGRESS',
                       pot               INTEGER NOT NULL,               -- copia do valor do lobby/match
                       results           Text NOT NULL               -- TODO: List of text ?
);

--CREATE INDEX ix_round_match ON round(match_id);

-- Cada jogador joga 1 turno por ronda; até 3 lançamentos

-- Histórico dos lançamentos dentro de um turno (1..3)
CREATE TABLE Dice (
                      id         BIGSERIAL PRIMARY KEY,
                      round_id   BIGINT  NOT NULL REFERENCES round(round_no) ON DELETE CASCADE,
                      user_id    BIGINT  NOT NULL REFERENCES dbo.users(id)    ON DELETE CASCADE,
                      roll_no    SMALLINT NOT NULL CHECK (roll_no BETWEEN 1 AND 3),
                      d1         dice_face NOT NULL,
                      d2         dice_face NOT NULL,
                      d3         dice_face NOT NULL,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                      UNIQUE (round_id, user_id, roll_no)
);

-- ===========================
-- Movimentos/Transações (saldo)
-- ===========================
CREATE TABLE wallet_tx (
                           id                BIGSERIAL PRIMARY KEY,
                           user_id           BIGINT NOT NULL REFERENCES dbo.users(id) ON DELETE CASCADE,
                           --match_id          BIGINT REFERENCES match(id) ON DELETE SET NULL,
                           round_id          BIGINT REFERENCES round(round_no) ON DELETE SET NULL,
                           type              tx_type NOT NULL,               -- ANTE (-), WIN (+), ADJUSTMENT (+/-)
                           amount_coins      INTEGER NOT NULL,               -- débito < 0, crédito > 0
                           created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                           CHECK (amount_coins <> 0)
);

CREATE INDEX ix_wallet_user ON wallet_tx(user_id);
CREATE INDEX ix_wallet_round ON wallet_tx(round_id);
