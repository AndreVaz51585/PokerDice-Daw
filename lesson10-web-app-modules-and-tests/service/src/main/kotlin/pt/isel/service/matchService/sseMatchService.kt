package pt.isel.service.matchService

import org.springframework.stereotype.Service
import pt.isel.domain.Game.pokerDice.Command
import pt.isel.service.Auxiliary.Either
import pt.isel.service.Auxiliary.failure
import pt.isel.service.Auxiliary.success


// Vamos definir o que é que um user deve ver dependendo dos comandos por ele executados
// Quando a partida começa, o ideal seria que todos vissem a ordem inicial para saberem quando é que jogam , e as ações que faltam
// Quando faz Roll , devemos mostrar o resultado do roll e respetivo valor associado a essa combinação , juntamente com com rerolls left e proximo jogador
// Quando faz Hold , devemos mostrar o estado atual dos dados , e deve retornar as novos resulatodos dos rolls nesses indices.
// Quand algum user tenta executar um comando , quando não é a sua vez , devemos retornar um erro indicando que não é a sua vez
// Quando o user der finishturn , passa ao proximo e as mesmas condições se aplicam
// Quando a ronda termina , devemos mostrar o resumo da ronda e os vencedores dessa ronda juntamente com o prémio ganho
// Isto deve-se seguir de ronda em ronda até que a partida termine , onde devemos mostrar o vencedor final e o prémio ganho
// Para além disso , devemos garantir que os users não vejam informações que não devem ver , como os dados dos outros jogadores , ou o valor total do pote aberto se este não estiver fechado ainda

@Service
class sseMatchService {

    fun getRawTypeFromBody(body: Map<String, Any>): String? =
       (body["type"] ?: body["action"])?.toString()?.lowercase()



    fun getIndicesFromBody(body: Map<String, Any>): Set<Int> {
        return when (val v = body["indices"]) {
            is List<*> -> v.mapNotNull { (it as? Number)?.toInt() }.toSet()
            is Array<*> -> v.mapNotNull { (it as? Number)?.toInt() }.toSet()
            else -> emptySet()
        }
    }

        fun executeComand(
            action: String,
            userId: Int,
            indices: Set<Int> = emptySet<Int>()
        ): Either<MatchServiceError, Command> {

            if (action == "hold") {
                if (indices.isEmpty()) return failure(MatchServiceError.CommandInvalidIndices)

                if (indices.any { it !in 0..4 }) return failure(MatchServiceError.CommandInvalidIndices)
            }

            val cmd: Command = when (action) {
                "roll" -> Command.Roll(userId)
                "hold" -> Command.Hold(userId, indices)
                "finish-turn" -> Command.FinishTurn(userId)
                else -> return failure(MatchServiceError.CommandUnknown)
            }

            return success(cmd)
        }


    }

