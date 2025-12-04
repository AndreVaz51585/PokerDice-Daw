package pt.isel.domain.Game.money.walletDTO

data class WithdrawSucess(
    val amountWidrwawn: Int,
    val currentBalance : Int,
    val message: String
)
