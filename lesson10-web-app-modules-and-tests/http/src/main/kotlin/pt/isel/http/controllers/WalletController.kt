package pt.isel.http.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pt.isel.domain.Game.money.AmountPayload
import pt.isel.domain.user.AuthenticatedUser
import pt.isel.http.model.problem.Problem
import pt.isel.service.Auxiliary.Failure
import pt.isel.service.Auxiliary.Success
import pt.isel.service.walletService.WalletService
import pt.isel.service.walletService.WalletServiceError


@RestController
class WalletController(
    private val walletService: WalletService,
) {
    @GetMapping("/api/wallets")
    fun listAllWallets(): ResponseEntity<Any> {
        return when (val result = walletService.getAll()) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é List<Wallet>
            }

            else -> {
                Problem.Unknown.response(
                    HttpStatus.BAD_REQUEST,
                )

            }
        }
    }

    @GetMapping("/api/wallets/{userId}")
    fun getWallet(
        user: AuthenticatedUser,
        @PathVariable userId: Int
    ): ResponseEntity<Any> {

        val result = walletService.getWallet(user.user.id, userId)

        return when (result) {

            is Success -> {
                ResponseEntity.ok(result.value)   // value é a wallet do User
            }

            is Failure -> when (result.value) {
                is WalletServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                WalletServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                WalletServiceError.WalletNotFound -> Problem.WalletNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                WalletServiceError.InvalidAmount -> Problem.InvalidAmount.response(
                    HttpStatus.BAD_REQUEST
                )
            }
        }
    }


    @PostMapping("/api/wallets/{userId}/deposit")
    fun deposit(
        user: AuthenticatedUser,
        @PathVariable userId: Int,
        @RequestBody payload: AmountPayload
    ): ResponseEntity<Any> {


        val result = walletService.deposit(user.user.id, userId, payload.amount)

        return when (result) {

            is Success -> ResponseEntity.ok(result.value.currentBalance)

            is Failure -> when (result.value) {
                is WalletServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                WalletServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                WalletServiceError.WalletNotFound -> Problem.WalletNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                WalletServiceError.InvalidAmount -> Problem.InvalidAmount.response(
                    HttpStatus.BAD_REQUEST
                )
            }
        }
    }

    @PostMapping("/api/wallets/{userId}/withdraw")
    fun withdraw(
        user: AuthenticatedUser,
        @PathVariable userId: Int,
        @RequestBody amount: AmountPayload
    ): ResponseEntity<Any> {

        return when (val result = walletService.withdraw(user.user.id, userId, amount.amount)) {

            is Success -> ResponseEntity.ok(result.value.currentBalance)

            is Failure -> when (result.value) {
                is WalletServiceError.NoPermission -> Problem.NoPermission.response(
                    HttpStatus.BAD_REQUEST,
                )

                WalletServiceError.UserNotFound -> Problem.UserNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                WalletServiceError.WalletNotFound -> Problem.WalletNotFound.response(
                    HttpStatus.BAD_REQUEST,
                )

                WalletServiceError.InvalidAmount -> Problem.InvalidAmount.response(
                    HttpStatus.BAD_REQUEST
                )
            }
        }
    }

}




