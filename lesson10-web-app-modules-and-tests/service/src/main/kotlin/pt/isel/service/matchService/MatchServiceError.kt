package pt.isel.service.matchService

sealed class MatchServiceError {
    data object MatchNotFound : MatchServiceError()

    data object InvalidState : MatchServiceError()

    // Erros relacionados com jogadores dentro do contexto do Match
    data object PlayerNotFound : MatchServiceError()

    data object PlayerAlreadyInMatch : MatchServiceError()

    data object MatchFull : MatchServiceError()

    data class Unknown(val reason: String) : MatchServiceError()
}