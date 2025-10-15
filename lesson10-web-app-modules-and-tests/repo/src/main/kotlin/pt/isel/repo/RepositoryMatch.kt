package pt.isel.repo

import pt.isel.domain.Game.Lobby.Lobby
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.domain.Game.Round.Round
import java.time.Instant

/**
 * Porta do domínio para persistência de Matches via repositório.
 * Mantém o domínio independente da infra (JDBI, JDBC, etc.).
 *
 * Ajusta os tipos (Int vs Int) conforme o teu DDL real.
 */
interface RepositoryMatch : Repository<Match> {
    fun exists(id: Int): Boolean

    /**
     * Cria um Match e os respetivos jogadores (transacional via unidade de trabalho externa).
     */
    fun createMatch(
        id: Int,
        lobbyId: Int,
        players: List<MatchPlayer>,
        totalRounds: Int,
        ante: Int,
        state: MatchState = MatchState.RUNNING,
        currentRoundNo: Int = 1,
        startedAt: Instant = Instant.now(),
        finishedAt: Instant? = null
    ): Match


    /**
     * Atualiza o estado e, opcionalmente, a finishedAt.
     */
    fun updateState(id: Int, newState: MatchState, finishedAt: Instant? = null): Boolean

//TODO: Lobby informação estatica
//TODO: Match jogo geral que vai possuir varias rondas e que é uma instancia do Lobby
//TODO: Rondas vai ser um numero especifico defenido por match

    /**
     * Atualiza o número da ronda corrente.
     */
    fun updateCurrentRound(id: Int, roundNo: Int): Boolean
    
    // Operações sobre jogadores
    fun listPlayers(matchId: Int): List<MatchPlayer>
    fun setPlayerActive(matchId: Int, userId: Int, active: Boolean): Int
    fun addPlayer(matchId: Int, player: MatchPlayer): Boolean
    fun removePlayer(matchId: Int, userId: Int): Boolean

}