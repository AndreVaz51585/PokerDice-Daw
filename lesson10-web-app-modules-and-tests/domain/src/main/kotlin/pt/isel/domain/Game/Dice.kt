package pt.isel.domain.Game

object Dice{

    // Funcão responsavél por calulcar o lançamento do dado
    fun roll() : Face = Face.entries.random()


}