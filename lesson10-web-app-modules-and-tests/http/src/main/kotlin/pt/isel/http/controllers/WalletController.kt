package pt.isel.http.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import pt.isel.service.Auxiliary.Success
import pt.isel.service.walletService.WalletService

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
}




