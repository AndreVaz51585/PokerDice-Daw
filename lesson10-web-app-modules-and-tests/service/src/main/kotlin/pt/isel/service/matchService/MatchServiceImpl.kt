package pt.isel.service.matchService

import jakarta.inject.Named
import org.springframework.stereotype.Service
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.domain.Game.pokerDice.Game
import pt.isel.domain.Game.money.Wallet
import pt.isel.repo.RepositoryMatch
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.failure
import pt.isel.service.Auxiliary.success
import java.time.Instant

@Service
class MatchServiceImpl(
    private val repoMatch: RepositoryMatch,
    private val trxManager: TransactionManager,
) : MatchService {

    override fun createMatch(
        lobbyId: Int,
        totalRounds: Int,
        ante: Int
    ): Either<MatchServiceError, Match> = trxManager.run {
        if (totalRounds <= 0 || ante < 0) {
            return@run failure(MatchServiceError.InvalidState)
        }
        // Evitar duplicados iniciais
        val match = repoMatch.createMatch(
            lobbyId = lobbyId,
            totalRounds = totalRounds,
            ante = ante,
        )
        return@run success(match)
    }

    override fun getMatch(id: Int): Either<MatchServiceError, Match> = trxManager.run {
        val match = repoMatch.findById(id) ?: return@run failure(MatchServiceError.MatchNotFound)
        success(match)
    }

    override fun addPlayer(matchId: Int, player: MatchPlayer): Either<MatchServiceError, Boolean> = trxManager.run {
        val match = repoMatch.findById(matchId) ?: return@run failure(MatchServiceError.MatchNotFound)

        val currentPlayers = repoMatch.listPlayers(matchId)
        if (currentPlayers.any { it.userId == player.userId }) {
            return@run failure(MatchServiceError.PlayerAlreadyInMatch)
        }
        match.maxPlayers.let { max ->
            if (currentPlayers.size >= max) return@run failure(MatchServiceError.MatchFull)
        }
        val ok = repoMatch.addPlayer(
            matchId,
            userId = player.userId,
            balanceAtStart = player.balanceAtStart,
            seatNo = repoMatch.getMaxSeatNo(match.id)+1
        )
        if (!ok) return@run failure(MatchServiceError.Unknown)
        success(true)
    }

    override fun removePlayer(matchId: Int, userId: Int): Either<MatchServiceError, Boolean> = trxManager.run {
        repoMatch.findById(matchId) ?: return@run failure(MatchServiceError.MatchNotFound)

        val players = repoMatch.listPlayers(matchId)
        if (players.none { it.userId == userId }) {
            return@run failure(MatchServiceError.PlayerNotFound)
        }
        val ok = repoMatch.removePlayer(matchId, userId)
        if (!ok) return@run failure(MatchServiceError.Unknown)

        // Optional: update gameState to reflect removal
        success(true)
    }

    override fun updateState(matchId: Int, newState: MatchState): Either<MatchServiceError, Boolean> = trxManager.run {
        val match = repoMatch.findById(matchId) ?: return@run failure(MatchServiceError.MatchNotFound)

        // Simple validation: cannot change state once finished
        if (match.state == MatchState.FINISHED) {
            return@run failure(MatchServiceError.InvalidState)
        }

        // If transitioning to RUNNING you may want to initialize or start the embedded game.
        // Currently this method persists the state change; if you want the start to also
        // cause domain behaviour (e.g. collect antes via GameEngine), add that logic here.
        val ok = repoMatch.updateState(matchId, newState)
        if (!ok) return@run failure(MatchServiceError.Unknown)

        success(true)
    }

    override fun listPlayers(matchId: Int): List<MatchPlayer> = trxManager.run {
        // Return players from repo (table). If you rather use match.gameState.players,
        // ensure gameState is kept up to date on every player add/remove.
        repoMatch.listPlayers(matchId)
    }
}
