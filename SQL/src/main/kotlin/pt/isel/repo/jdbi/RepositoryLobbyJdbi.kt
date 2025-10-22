package pt.isel.repo.jdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.slf4j.LoggerFactory
import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Lobby.LobbyState
import pt.isel.domain.user.User
import pt.isel.repo.RepositoryLobby
import pt.isel.repo.jdbi.sql.LobbySql

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
              createQuery(LobbySql.SELECT_BY_ID)
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
        handle.createQuery(LobbySql.SELECT_ALL)
            .mapTo<Lobby>()
            .list()

    /**
     * Updates an existing Lobby in the database.
     * @param entity The Lobby entity to be updated.
     *
     * */
    override fun save(entity: Lobby) {
        handle.createUpdate(
            LobbySql.UPDATE_LOBBY,
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
        val ret = handle.createUpdate(LobbySql.DELETE_BY_ID)
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
        handle.createUpdate(LobbySql.CLEAR_LOBBIES).execute()
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
                LobbySql.CREATE_LOBBY
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
        handle.createQuery(LobbySql.SELECT_HOST)
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
        handle.createQuery(LobbySql.SELECT_OPEN_LOBBIES_PAGED
        )
            .bind("state", LobbyState.OPEN.name)
            .bind("limit", limit)
            .bind("offset", offset)
            .mapTo<Lobby>()
            .list()


    override fun addPlayerToLobby(lobbyId: Int, playerId: Int): Boolean {
        val rows = handle.createUpdate(
          LobbySql.ADD_PLAYER
        )
            .bind("lobbyId", lobbyId)
            .bind("userId", playerId)
            .execute()
        return rows > 0

    }

    override fun remove(lobbyId: Int, userId: Int) : Boolean {
        val rows = handle.createUpdate(
            LobbySql.REMOVE_PLAYER
        )
            .bind("lobbyId", lobbyId)
            .bind("userId", userId)
            .execute()
        return rows > 0
    }
    override fun listPlayers(lobbyId: Int): List<User> =
        handle.createQuery(
            LobbySql.SELECT_PLAYERS
        )
            .bind("lobbyId", lobbyId)
            .mapTo<User>()
            .list()


    override fun countPlayers(lobbyId: Int): Int =
        handle.createQuery(
          LobbySql.COUNT_PLAYERS
        )
            .bind("lobbyId", lobbyId)
            .mapTo(Int::class.java)
            .one()

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryLobbyJdbi::class.java)
    }

}