package com.whattoeat.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    private val engine = GameEngine()
    var state by mutableStateOf(engine.state)
        private set
    var chinese by mutableStateOf(false)
        private set
    var playerCount by mutableStateOf(2)
    var names by mutableStateOf(List(8) { "" })
        private set
    var setupFoods by mutableStateOf(defaultFoods)
        private set
    fun setName(index: Int, value: String) { names = names.toMutableList().also { it[index] = value } }
    fun removeFood(id: String) {
        if (setupFoods.size > 1) setupFoods = setupFoods.filterNot { it.id == id }
    }
    fun addFood(name: String) {
        val clean = name.trim()
        if (clean.isBlank() || setupFoods.size >= 12 || setupFoods.any { it.english.equals(clean, true) || it.chinese == clean }) return
        setupFoods = setupFoods + FoodOption("custom-" + java.util.UUID.randomUUID(), clean, clean, "🍽")
    }
    fun restoreFoods() { setupFoods = defaultFoods }
    fun toggleLanguage() { chinese = !chinese }
    private fun update(action: () -> Unit) { action(); state = engine.state }
    fun start(names: List<String>, foods: List<FoodOption>) = update { engine.start(names, foods) }
    fun spin() = update { engine.spin() }
    fun animationFinished() = update { engine.animationFinished() }
    fun vote(vote: Vote) = update { engine.vote(vote) }
    fun follow() = update { engine.follow() }
    fun finishTurn() = update { engine.finishTurn() }
    fun nextPlayer() = update { engine.nextPlayer() }
    fun replay() = update { engine.replay() }
    fun reset() = update { engine.reset() }
}
