package pt.isel.service.roundService

sealed class RoundServiceError {
    data object RoundNotFound : RoundServiceError()
    data object MatchNotFound : RoundServiceError()
    data object InvalidRoundState : RoundServiceError()
    data object PlayerNotInMatch : RoundServiceError()
    data object InsufficientCoins : RoundServiceError()
    data object InvalidHandSubmission : RoundServiceError()
    data object ErrorSubmittingHand : RoundServiceError()
    data object RoundAlreadyClosed : RoundServiceError()
    data object ErrorCalculatingWinners : RoundServiceError()
    data object ErrorClosingRound : RoundServiceError()
}
