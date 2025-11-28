package pt.isel.repo.jdbi.sql

object LobbySql {
    const val SELECT_BY_ID = """
        SELECT * FROM lobby WHERE id = :id
    """

    const val SELECT_ALL = """
        SELECT * FROM lobby
    """

    const val UPDATE_LOBBY = """
        UPDATE lobby
        SET name = :name,
            description = :description,
            min_players = :minPlayers,
            max_players = :maxPlayers,
            rounds = :rounds,
            ante = :ante,
            state = CAST(:state AS lobby_state)
        WHERE id = :id
    """

    const val DELETE_BY_ID = """
        DELETE FROM lobby WHERE id = :id
    """

    const val CLEAR_LOBBY_PLAYERS = """
        DELETE FROM lobby_player
    """

    const val CLEAR_LOBBIES = """
        DELETE FROM lobby
    """

    const val CREATE_LOBBY = """
        INSERT INTO lobby (lobby_Host, name, description, min_players, max_players, rounds, ante, state) 
        VALUES (:lobbyHostId, :name, :description, :minPlayers, :maxPlayers, :rounds, :ante, CAST(:state AS lobby_state))
        RETURNING id
    """

    const val SELECT_HOST = """
        SELECT * FROM dbo.users WHERE id = :id
    """

    const val SELECT_OPEN_LOBBIES_PAGED = """
        SELECT * FROM lobby
        WHERE state = CAST(:state AS lobby_state)
        ORDER BY id
        LIMIT :limit OFFSET :offset
    """

    const val ADD_PLAYER = """
        INSERT INTO lobby_player (lobby_id, user_id)
        VALUES (:lobbyId, :userId)
        ON CONFLICT DO NOTHING
    """

    const val REMOVE_PLAYER = """
        DELETE FROM lobby_player
        WHERE lobby_id = :lobbyId AND user_id = :userId
    """

    const val SELECT_PLAYERS = """
        SELECT u.*
        FROM dbo.users u
        INNER JOIN lobby_player lp ON lp.user_id = u.id
        WHERE lp.lobby_id = :lobbyId
        ORDER BY lp.joined_at
    """

    const val COUNT_PLAYERS = """
        SELECT COUNT(*) FROM lobby_player
        WHERE lobby_id = :lobbyId
    """
}
