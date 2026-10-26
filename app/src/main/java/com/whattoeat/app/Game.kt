package com.whattoeat.app

import kotlin.random.Random

data class FoodOption(val id: String, val english: String, val chinese: String, val symbol: String) {
    fun label(chineseLanguage: Boolean) = if (chineseLanguage) chinese else english
}
val defaultFoods = listOf(
    FoodOption("japanese", "Japanese", "日本菜", "🍣"),
    FoodOption("chinese", "Chinese", "中菜", "🥟"),
    FoodOption("western", "Western", "西餐", "🍝"),
    FoodOption("korean", "Korean", "韓國菜", "🍲"),
    FoodOption("thai", "Thai", "泰國菜", "🍜"),
    FoodOption("hot-pot", "Hot Pot", "火鍋", "🥘"),
    FoodOption("pizza", "Pizza", "薄餅", "🍕"),
    FoodOption("burgers", "Burgers", "漢堡", "🍔"),
    FoodOption("indian", "Indian", "印度菜", "🍛")
)
enum class Vote(val score: Int) { LIKE(3), SKIP(0), REJECT(-3) }
data class Player(val name: String, val votes: Map<String, Vote> = emptyMap(), val follows: Boolean = false)
data class Recommendation(
    val food: FoodOption, val likes: Int, val rejects: Int, val skips: Int,
    val unknown: Int, val unanimous: Boolean
) { val compromise get() = rejects > 0 }
data class MatchResult(val top: List<Recommendation>, val randomFood: FoodOption? = null)

object Matcher {
    fun calculate(foods: List<FoodOption>, players: List<Player>, random: Random = Random.Default): MatchResult {
        require(foods.isNotEmpty() && foods.map { it.id }.distinct().size == foods.size)
        require(players.isNotEmpty())
        val active = players.filterNot { it.follows }
        if (active.isEmpty()) return MatchResult(emptyList(), foods.random(random))
        val ranked = foods.map { food ->
            val votes = active.mapNotNull { it.votes[food.id] }
            val likes = votes.count { it == Vote.LIKE }
            Recommendation(food, likes, votes.count { it == Vote.REJECT },
                votes.count { it == Vote.SKIP }, active.size - votes.size,
                likes == active.size)
        }.sortedWith(compareBy<Recommendation> { it.rejects > 0 }
            .thenBy { it.rejects }.thenByDescending { it.likes })
        // Stable sorting leaves exact ties in the original setup order.
        return MatchResult(ranked.take(3))
    }
}

enum class Screen { SETUP, ROULETTE, PASS, RESULTS }
data class GameState(
    val screen: Screen = Screen.SETUP,
    val foods: List<FoodOption> = defaultFoods,
    val players: List<Player> = emptyList(),
    val current: Int = 0,
    val spins: Int = 0,
    val pendingId: String? = null,
    val spinning: Boolean = false,
    val turnDone: Boolean = false,
    val result: MatchResult? = null
) {
    val player get() = players.getOrNull(current)
    val spinLimit get() = minOf(3, foods.size)
    val canSpin get() = screen == Screen.ROULETTE && !spinning && !turnDone && pendingId == null && spins < spinLimit
    val canFollow get() = screen == Screen.ROULETTE && spins == 0 && !spinning && !turnDone
}

/** Pure local turn state machine; Android-independent so it can be unit tested. */
class GameEngine(private val random: Random = Random.Default) {
    var state = GameState()
        private set
    fun start(names: List<String>, foods: List<FoodOption>) {
        require(names.size in 1..8 && names.all { it.isNotBlank() })
        require(foods.isNotEmpty() && foods.size <= 12 && foods.map { it.id }.distinct().size == foods.size)
        require(foods.all { it.id.isNotBlank() && it.english.isNotBlank() && it.chinese.isNotBlank() })
        state = GameState(screen = Screen.ROULETTE, foods = foods.toList(), players = names.map { Player(it) })
    }
    fun spin() {
        if (!state.canSpin) return
        val available = state.foods.filterNot { state.player!!.votes.containsKey(it.id) }
        state = state.copy(pendingId = available.random(random).id, spins = state.spins + 1, spinning = true)
    }
    fun animationFinished() {
        if (state.spinning) state = state.copy(spinning = false)
    }
    fun vote(vote: Vote) {
        val id = state.pendingId ?: return
        if (state.spinning || state.turnDone || state.screen != Screen.ROULETTE) return
        val updated = state.player!!.copy(votes = state.player!!.votes + (id to vote))
        state = state.copy(players = state.players.toMutableList().also { it[state.current] = updated },
            pendingId = null, turnDone = vote == Vote.LIKE || state.spins >= state.spinLimit)
    }
    fun follow() {
        if (!state.canFollow) return
        state = state.copy(players = state.players.toMutableList().also {
            it[state.current] = it[state.current].copy(follows = true)
        }, turnDone = true)
        finishTurn()
    }
    fun finishTurn() {
        if (!state.turnDone || state.screen != Screen.ROULETTE) return
        state = if (state.current == state.players.lastIndex) {
            state.copy(screen = Screen.RESULTS, result = Matcher.calculate(state.foods, state.players, random))
        } else state.copy(screen = Screen.PASS)
    }
    fun nextPlayer() {
        if (state.screen != Screen.PASS) return
        state = state.copy(screen = Screen.ROULETTE, current = state.current + 1,
            spins = 0, pendingId = null, spinning = false, turnDone = false)
    }
    fun replay() = start(state.players.map { it.name }, state.foods)
    fun reset() { state = GameState() }
}
