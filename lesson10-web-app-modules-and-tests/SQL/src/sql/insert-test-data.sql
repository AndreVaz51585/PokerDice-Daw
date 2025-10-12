BEGIN;

WITH inserted_user AS (
    INSERT INTO app_user (username, password_hash)
    VALUES ('andre', 'hashedpass123')
    RETURNING id
)
INSERT INTO lobby (lobby_host, name, description, min_players, max_players, rounds)
SELECT id, 'Test Lobby', 'Demo match', 2, 4, 3
FROM inserted_user;

COMMIT;
