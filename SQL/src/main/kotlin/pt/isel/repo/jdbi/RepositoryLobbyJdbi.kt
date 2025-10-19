package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.slf4j.LoggerFactory
import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Lobby.LobbyState
import pt.isel.domain.user.User
import pt.isel.repo.RepositoryLobby

class RepositoryLobbyJdbi(
    private val handle : Handle
) : RepositoryLobby{

    /**
     * Finds a Lobby by its ID.
     * @param id The ID of the Lobby to be retrieved.
     * @return The Lobby entity if found, otherwise null.
     *
     * */
    override fun findById(id: Int): Lobby? =
        handle.
              createQuery("SELECT * FROM lobby WHERE id = :id")
              .bind("id", id)
              .mapTo<Lobby>()
              .findOne()
              .orElse(null)

    /**
     * Retrieves all Lobbies from the database.
     * @return A list of all Lobby entities.
     *
     * */
    override fun findAll(): List<Lobby> =
        handle.createQuery("SELECT * FROM lobby")
            .mapTo<Lobby>()
            .list()

    /**
     * Updates an existing Lobby in the database.
     * @param entity The Lobby entity to be updated.
     *
     * */
    override fun save(entity: Lobby) {
        handle.createUpdate(
            """
            UPDATE lobby
            SET name = :name,
                description = :description,
                min_Players = :minPlayers,
                max_Players = :maxPlayers,
                rounds = :rounds,
                ante = :ante,
                state = CAST(:state AS lobby_state)
            WHERE id = :id
            """
        )
            .bind("id", entity.id)
            .bind("name", entity.name)
            .bind("description", entity.description)
            .bind("minPlayers", entity.minPlayers)
            .bind("maxPlayers", entity.maxPlayers)
            .bind("rounds", entity.rounds)
            .bind("ante", entity.ante)
            .bind("state", entity.state.name)
            .execute()
    }

    /**
     * Deletes a Lobby by its ID.
     * If has LobbyPlayers, they should be deleted first.
     * @param id The ID of the Lobby to be deleted.
     *
     * */
    override fun deleteById(id: Int): Boolean {
        // relevante caso adicionemos a lista de players no lobby
       /* handle.createQuery("DELETE FROM dbo.LobbyPlayers WHERE lobbyId = :id")
            .bind("id", id)
            .execute()*/
        val ret = handle.createUpdate("DELETE FROM lobby WHERE id = :id")
            .bind("id", id)
            .execute() > 0
        return ret
    }

     /**
      * Deletes all entries from the Lobbies table.
      * Note: This operation does not cascade to related tables.
      *
      * */
    override fun clear() {
       // handle.createUpdate("DELETE FROM dbo.LobbyMembers").execute()
        handle.createUpdate("DELETE FROM lobby").execute()
    }

    /**
     * Creates a new Lobby in the database.
     * @param lobbyHostId The ID of the user hosting the lobby.
     * @param name The name of the lobby.
     * @param description A description of the lobby.
     * @param minPlayers The minimum number of players required to start the game.
     * @param maxPlayers The maximum number of players allowed in the lobby.
     * @param rounds The number of rounds to be played in the game.
     * @param ante The ante amount for each round.
     * @param state The current state of the lobby (e.g., OPEN, CLOSED).
     * @return The created Lobby entity with its generated ID.
     *
     * */

    override fun createLobby(
        lobbyHostId: Int,
        name: String,
        description: String,
        minPlayers: Int,
        maxPlayers: Int,
        rounds: Int,
        ante: Int
    ): Lobby {
        val id = handle
            .createUpdate(
                """
            INSERT INTO lobby (lobby_Host,name, description, min_Players, max_Players, rounds, ante,state) 
            VALUES (:lobbyHostId, :name, :description,:minPlayers, :maxPlayers, :rounds, :ante, CAST(:state AS lobby_state))
            RETURNING id
            """,
            )
            .bind("lobbyHostId", lobbyHostId)
            .bind("name", name)
            .bind("description", description)
            .bind("minPlayers", minPlayers)
            .bind("maxPlayers", maxPlayers)
            .bind("rounds", rounds)
            .bind("ante", ante)
            .bind("state", LobbyState.OPEN.name)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .one()

        //no create-shcema já temos um trigger que adiciona manualmente o host à lista de players do lobby
        // porratnto aqui fazemo-lo para ficar coerente

        val host : User =
            handle.createQuery("SELECT * FROM dbo.users WHERE id = :id")
                .bind("id", lobbyHostId)
                .mapTo<User>()
                .one()

        return Lobby(
            id = id,
            lobbyHost = lobbyHostId,
            name = name,
            description = description,
            minPlayers = minPlayers,
            maxPlayers = maxPlayers,
            rounds = rounds,
            ante = ante,
            state = LobbyState.OPEN
        )
    }

    /**
     * Retrieves the host user of a given Lobby.
     * @param lobby The Lobby entity whose host is to be retrieved.
     * @return The User entity representing the lobby host, or null if not found.
     *
     * */
    override fun getLobbyHost(lobby: Lobby): User? =
        handle.createQuery("SELECT * FROM dbo.users WHERE id = :id")
            .bind("id", lobby.lobbyHost)
            .mapTo<User>()
            .findOne()
            .orElse(null)


    /***
     * Lists all open lobbies with pagination.
     * @param limit The maximum number of lobbies to retrieve.
     * @param offset The number of lobbies to skip before starting to collect the result set
     * @return A list of open Lobby entities.
     */

    override fun listAllOpenLobbies(
        limit: Int,
        offset: Int
    ): List<Lobby> =
        handle.createQuery(  """
            SELECT * FROM lobby
            WHERE state = CAST(:state AS lobby_state)
            ORDER BY id
            LIMIT :limit OFFSET :offset
            """
        )
            .bind("state", LobbyState.OPEN.name)
            .bind("limit", limit)
            .bind("offset", offset)
            .mapTo<Lobby>()
            .list()


    override fun addPlayerToLobby(lobbyId: Int, playerId: Int): Boolean {
        val rows = handle.createUpdate(
            """
            INSERT INTO lobby_player (lobby_id, user_id)
            VALUES (:lobbyId, :userId)
            ON CONFLICT DO NOTHING
            """
        )
            .bind("lobbyId", lobbyId)
            .bind("userId", playerId)
            .execute()
        return rows > 0

    }

    override fun remove(lobbyId: Int, userId: Int) : Boolean {
        val rows = handle.createUpdate(
            """
            DELETE FROM lobby_player
            WHERE lobby_id = :lobbyId AND user_id = :userId
            """
        )
            .bind("lobbyId", lobbyId)
            .bind("userId", userId)
            .execute()
        return rows > 0
    }
    override fun listPlayers(lobbyId: Int): List<User> =
        handle.createQuery(
            """
            SELECT u.*
            FROM dbo.users u
            INNER JOIN lobby_player lp ON lp.user_id = u.id
            WHERE lp.lobby_id = :lobbyId
            ORDER BY lp.joined_at
            """
        )
            .bind("lobbyId", lobbyId)
            .mapTo<User>()
            .list()


    override fun countPlayers(lobbyId: Int): Int =
        handle.createQuery(
            """
            SELECT COUNT(*) FROM lobby_player
            WHERE lobby_id = :lobbyId
            """
        )
            .bind("lobbyId", lobbyId)
            .mapTo(Int::class.java)
            .one()

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryLobbyJdbi::class.java)
    }

}