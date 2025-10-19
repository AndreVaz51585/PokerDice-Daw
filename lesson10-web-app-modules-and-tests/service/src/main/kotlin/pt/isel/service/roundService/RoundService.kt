package pt.isel.service.roundService

import pt.isel.domain.Game.Hand
import pt.isel.domain.Game.Round.Round
import pt.isel.domain.Game.Round.RoundState
import pt.isel.service.Auxiliary.Either
import java.time.Instant

interface RoundService {

    fun createRound(matchId: Long, number: Int, anteCoins: Int): Either<RoundServiceError, Round>

    fun getRound(id: Long): Either<RoundServiceError, Round>

    fun listRoundsByMatch(matchId: Long): List<Round>

    fun getCurrentRound(matchId: Long): Either<RoundServiceError, Round>

    fun submitHand(roundId: Long, playerId: Int, hand: Hand): Either<RoundServiceError, Boolean>

    fun scoreRound(roundId: Long): Either<RoundServiceError, Round>

    fun closeRound(roundId: Long): Either<RoundServiceError, Round>

    fun updateRoundState(roundId: Long, state: RoundState): Either<RoundServiceError, Round>

    fun getRoundWinners(roundId: Long): Either<RoundServiceError, List<Int>>

    fun getRoundHands(roundId: Long): Either<RoundServiceError, Map<Int, Hand>>
}
