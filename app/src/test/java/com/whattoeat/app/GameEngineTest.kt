package com.whattoeat.app

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    private fun game(players: Int = 1, foods: List<FoodOption> = defaultFoods) = GameEngine(Random(17)).apply {
        start((1..players).map { "Player $it" }, foods)
    }
    private fun voteNext(engine: GameEngine, vote: Vote): String {
        engine.spin()
        val id = engine.state.pendingId!!
        engine.animationFinished()
        engine.vote(vote)
        return id
    }

    @Test fun threeSpinsAreDistinctAndThenTurnEnds() {
        val engine = game()
        val seen = (1..3).map { voteNext(engine, Vote.SKIP) }
        assertEquals(3, seen.toSet().size)
        assertTrue(engine.state.turnDone)
        engine.spin()
        assertEquals(3, engine.state.spins)
        assertNull(engine.state.result)
        engine.finishTurn()
        assertEquals(Screen.RESULTS, engine.state.screen)
    }

    @Test fun repeatedSpinAndVoteDuringAnimationAreIgnored() {
        val engine = game()
        engine.spin()
        val initial = engine.state
        engine.spin()
        engine.vote(Vote.LIKE)
        engine.follow()
        engine.finishTurn()
        assertEquals(initial, engine.state)
        engine.animationFinished()
        engine.spin() // Must vote before another spin.
        assertEquals(1, engine.state.spins)
        engine.vote(Vote.LIKE)
        engine.vote(Vote.REJECT) // No double voting.
        assertEquals(listOf(Vote.LIKE), engine.state.player!!.votes.values.toList())
    }

    @Test fun likeAllowsEarlyFinishAndNoMoreSpins() {
        val engine = game()
        voteNext(engine, Vote.LIKE)
        assertTrue(engine.state.turnDone)
        assertFalse(engine.state.canSpin)
        engine.finishTurn()
        assertEquals(Screen.RESULTS, engine.state.screen)
    }

    @Test fun rejectLetsPlayerContinueButNotFollowAfterVoting() {
        val engine = game()
        voteNext(engine, Vote.REJECT)
        assertTrue(engine.state.canSpin)
        assertFalse(engine.state.canFollow)
        engine.follow()
        assertFalse(engine.state.player!!.follows)
        assertEquals(Screen.ROULETTE, engine.state.screen)
    }

    @Test fun passScreenPreventsActionsAndResultsStayHiddenUntilLastPlayer() {
        val engine = game(2)
        voteNext(engine, Vote.LIKE)
        engine.finishTurn()
        assertEquals(Screen.PASS, engine.state.screen)
        assertNull(engine.state.result)
        val paused = engine.state
        engine.spin()
        engine.vote(Vote.REJECT)
        engine.follow()
        engine.finishTurn()
        assertEquals(paused, engine.state)
        engine.nextPlayer()
        assertEquals(1, engine.state.current)
        assertEquals(0, engine.state.spins)
        assertNull(engine.state.pendingId)
        assertTrue(engine.state.player!!.votes.isEmpty())
        engine.follow()
        assertEquals(Screen.RESULTS, engine.state.screen)
        assertNotNull(engine.state.result)
    }

    @Test fun eightFollowersGetRandomResultOnlyAfterLastTurn() {
        val engine = game(8)
        repeat(8) { index ->
            engine.follow()
            if (index < 7) {
                assertEquals(Screen.PASS, engine.state.screen)
                assertNull(engine.state.result)
                engine.nextPlayer()
            }
        }
        assertEquals(Screen.RESULTS, engine.state.screen)
        assertNotNull(engine.state.result!!.randomFood)
        assertTrue(engine.state.players.all { it.follows && it.votes.isEmpty() })
    }

    @Test fun smallMenusFinishWhenNoUnseenChoicesRemain() {
        for (size in 1..2) {
            val engine = game(foods = defaultFoods.take(size))
            repeat(size) { voteNext(engine, Vote.REJECT) }
            assertTrue(engine.state.turnDone)
            assertEquals(size, engine.state.spinLimit)
            engine.finishTurn()
            assertEquals(size, engine.state.result!!.top.size)
        }
    }

    @Test fun replayKeepsSetupButClearsVotesAndResetReturnsHome() {
        val engine = game(2, defaultFoods.take(2))
        engine.follow(); engine.nextPlayer(); engine.follow()
        engine.replay()
        assertEquals(Screen.ROULETTE, engine.state.screen)
        assertEquals(2, engine.state.players.size)
        assertEquals(defaultFoods.take(2), engine.state.foods)
        assertTrue(engine.state.players.all { !it.follows && it.votes.isEmpty() })
        assertNull(engine.state.result)
        engine.reset()
        assertEquals(GameState(), engine.state)
    }

    @Test fun defaultFoodsHaveStableUniqueIds() {
        assertEquals(9, defaultFoods.size)
        assertEquals(9, defaultFoods.map { it.id }.toSet().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun ninePlayersAreRejected() { game(9) }

    @Test(expected = IllegalArgumentException::class)
    fun zeroPlayersAreRejected() { game(0) }

    @Test(expected = IllegalArgumentException::class)
    fun blankNamesAreRejected() { GameEngine().start(listOf(" "), defaultFoods) }

    @Test(expected = IllegalArgumentException::class)
    fun emptyMenuIsRejected() { game(foods = emptyList()) }

    @Test(expected = IllegalArgumentException::class)
    fun blankIdsAreRejected() { game(foods = listOf(defaultFoods[0].copy(id = ""))) }
}
