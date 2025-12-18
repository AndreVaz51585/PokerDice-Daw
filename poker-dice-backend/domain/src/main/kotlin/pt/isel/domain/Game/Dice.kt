package pt.isel.domain.Game

import kotlin.random.Random

object Dice {
    // Permite injetar um RNG (testes determinísticos)
    fun roll(rng: Random = Random.Default): Face = Face.entries.random(rng)
}
