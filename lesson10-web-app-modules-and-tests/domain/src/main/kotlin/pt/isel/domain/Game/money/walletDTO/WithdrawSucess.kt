package pt.isel.domain.Game.money.walletDTO

data class WithdrawSucess(
    val amountWithdrawn: Int,
    val currentBalance : Int,
    val message: String
)
