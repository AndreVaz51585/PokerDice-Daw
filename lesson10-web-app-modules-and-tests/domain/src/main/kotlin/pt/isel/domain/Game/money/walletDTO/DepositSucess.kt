package pt.isel.domain.Game.money.walletDTO

data class DepositSucess(
    val deposited: Int,
    val currentBalance: Int,
    val message: String,
)
