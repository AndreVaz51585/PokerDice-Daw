package pt.isel.repo

import pt.isel.domain.Game.Round.Round
import pt.isel.domain.Game.Round.RoundState
import java.time.Instant

interface RepositoryRound : Repository<Round> {

    fun createRound(
        matchId: Int,
        number: Int,
        anteCoins: Int,
        startedAt: Instant
    ): Round

    // Encontrar rounds por match
    fun findByMatchId(matchId: Long): List<Round>

    // Encontrar o round atual de uma partida
    fun findCurrentRoundByMatchId(matchId: Long): Round?

    // Iniciar um round (atualiza timestamps e estado)
    fun startRound(roundId: Long): Boolean

    // Finalizar um round e definir vencedor
    fun completeRound(roundId: Long, winnerUserId: Int): Boolean

    // Atualizar o estado de um round
    fun updateState(roundId: Long, newState: RoundState): Boolean

    // Adicionar moedas ao pote
    fun addToPot(roundId: Long, amount: Int): Boolean

    // Obter o pote atual
    fun getPotAmount(roundId: Long): Int

    // Verificar se todos os jogadores já jogaram neste round
    fun allPlayersPlayed(roundId: Long): Boolean

    // Obter informações completas do round incluindo mãos dos jogadores
    fun getRoundWithHands(roundId: Long): Round

    // Verificar se um jogador específico já jogou neste round
    fun hasPlayerPlayed(roundId: Long, userId: Int): Boolean
}
