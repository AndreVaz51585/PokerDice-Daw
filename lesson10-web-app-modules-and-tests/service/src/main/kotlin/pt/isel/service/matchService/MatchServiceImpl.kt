package pt.isel.service.matchService

import jakarta.inject.Named
import org.springframework.stereotype.Service
import pt.isel.domain.Game.Match.Match
import pt.isel.domain.Game.Match.MatchPlayer
import pt.isel.domain.Game.Match.MatchState
import pt.isel.repo.RepositoryMatch
import pt.isel.repo.TransactionManager
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.failure
import pt.isel.service.Auxiliary.success
import java.io.Serial

@Service
class MatchServiceImpl(
    private val repoMatch: RepositoryMatch,
    private val trxManager: TransactionManager,
) : MatchService {

    override fun createMatch(
        id: Int,
        lobbyId: Int,
        players: List<MatchPlayer>,
        totalRounds: Int,
        ante: Int
    ): Either<MatchServiceError, Match> = trxManager.run {
        if (players.isEmpty() || totalRounds <= 0 || ante < 0) {
            return@run failure(MatchServiceError.InvalidState)
        }
        // Evitar duplicados iniciais
        if (players.map { it.userId }.distinct().size != players.size) {
            return@run failure(MatchServiceError.PlayerAlreadyInMatch)
        }
        val match = repoMatch.createMatch(
            lobbyId = lobbyId,
            players = players,
            totalRounds = totalRounds,
            ante = ante,
            id = id
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
        match.maxPlayers?.let { max ->
            if (currentPlayers.size >= max) return@run failure(MatchServiceError.MatchFull)
        }
        val ok = repoMatch.addPlayer(matchId, player)
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
        success(true)
    }

    override fun updateState(matchId: Int, newState: MatchState): Either<MatchServiceError, Boolean> = trxManager.run {
        val match = repoMatch.findById(matchId) ?: return@run failure(MatchServiceError.MatchNotFound)

        // Exemplo simples de validação de transição
        if (match.state == MatchState.FINISHED) {
            return@run failure(MatchServiceError.InvalidState)
        }
        val ok = repoMatch.updateState(matchId, newState)
        if (!ok) return@run failure(MatchServiceError.Unknown)
        success(true)
    }

    override fun listPlayers(matchId: Int): List<MatchPlayer> = trxManager.run {
        // Opcional: validar existência do match
        repoMatch.listPlayers(matchId)
    }
}
