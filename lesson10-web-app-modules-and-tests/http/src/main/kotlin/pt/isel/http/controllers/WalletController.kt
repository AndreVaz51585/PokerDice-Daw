package pt.isel.http.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pt.isel.service.Auxiliary.Success
import pt.isel.service.walletService.WalletService
import pt.isel.domain.Game.money.AmountPayload
import pt.isel.domain.user.AuthenticatedUser


@RestController
class WalletController(
    private val walletService: WalletService,

){
    @GetMapping("/api/wallets")
    fun listAllWallets(): ResponseEntity<Any> {
        return when (val result = walletService.getAll()) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é List<Wallet>
            }

            else -> {ResponseEntity.badRequest().body("")}
        }
    }

    @PostMapping("/api/wallets/{userId}/deposit")
    fun deposit(
        user: AuthenticatedUser,
        @PathVariable userId: Int,
        @RequestBody payload: AmountPayload
    ): ResponseEntity<Any> {
        val result = walletService.deposit(userId, payload.amount)

        return when (result) {

            is Success -> ResponseEntity.ok(result.value)

            else -> ResponseEntity.internalServerError().body("Erro ao depositar")
        }
    }

    @PostMapping("/api/wallets/{userId}/withdraw")
    fun withdraw(
        user: AuthenticatedUser,
        @PathVariable userId: Int,
        @RequestBody amount: AmountPayload
    ): ResponseEntity<Any> {

        return when (val result = walletService.withdraw(userId, amount.amount)) {

            is Success -> ResponseEntity.ok(result.value)

            else -> ResponseEntity.badRequest().body("Erro ao levantar")
        }
    }

}




