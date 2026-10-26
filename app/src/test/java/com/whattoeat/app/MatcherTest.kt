package com.whattoeat.app

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class MatcherTest {
    private val foods = defaultFoods.take(4)
    private val a = foods[0].id
    private val b = foods[1].id
    private val c = foods[2].id

    @Test fun zeroRejectsOutrankMoreLikesWithAReject() {
        val result = Matcher.calculate(foods, listOf(
            Player("A", mapOf(a to Vote.LIKE, b to Vote.LIKE)),
            Player("B", mapOf(a to Vote.REJECT, b to Vote.SKIP)),
            Player("C", mapOf(a to Vote.LIKE))
        ))
        assertEquals(b, result.top.first().food.id)
        assertFalse(result.top.any { it.food.id == a })
    }

    @Test fun eligibleOptionsRankByLikes() {
        val result = Matcher.calculate(foods, listOf(
            Player("A", mapOf(b to Vote.LIKE)), Player("B", mapOf(b to Vote.LIKE, a to Vote.LIKE))
        ))
        assertEquals(listOf(b, a, c), result.top.map { it.food.id })
        assertTrue(result.top.first().unanimous)
    }

    @Test fun unknownIsSeparateFromSkipAndDoesNotCountAsApproval() {
        val rec = Matcher.calculate(foods, listOf(
            Player("A", mapOf(a to Vote.LIKE)), Player("B", mapOf(a to Vote.SKIP)), Player("C")
        )).top.first()
        assertEquals(1, rec.likes)
        assertEquals(1, rec.skips)
        assertEquals(1, rec.unknown)
        assertFalse(rec.unanimous)
    }

    @Test fun followersContributeNothingEvenIfGivenStaleVotes() {
        val rec = Matcher.calculate(foods, listOf(
            Player("A", mapOf(a to Vote.LIKE)),
            Player("B", mapOf(a to Vote.REJECT), follows = true)
        )).top.first()
        assertEquals(0, rec.rejects)
        assertEquals(0, rec.unknown)
        assertTrue(rec.unanimous) // Explicit approval by every relevant (voting) player.
    }

    @Test fun allRejectedRanksFewestRejectsThenMostLikesAndLabelsCompromises() {
        val result = Matcher.calculate(foods.take(3), listOf(
            Player("A", mapOf(a to Vote.REJECT, b to Vote.REJECT, c to Vote.REJECT)),
            Player("B", mapOf(a to Vote.REJECT, b to Vote.LIKE)),
            Player("C", mapOf(a to Vote.LIKE, b to Vote.LIKE, c to Vote.LIKE))
        ))
        assertEquals(listOf(b, c, a), result.top.map { it.food.id })
        assertTrue(result.top.all { it.compromise })
        assertTrue(result.top.none { it.unanimous })
    }

    @Test fun fewerThanThreeSafeOptionsStillLabelsRejectedFallback() {
        val result = Matcher.calculate(foods.take(3), listOf(Player("A", mapOf(c to Vote.REJECT))))
        assertFalse(result.top[0].compromise)
        assertFalse(result.top[1].compromise)
        assertTrue(result.top[2].compromise)
    }

    @Test fun exactTiesUseStableMenuOrderWithoutClaimingApproval() {
        val result = Matcher.calculate(foods, listOf(Player("A", mapOf(a to Vote.SKIP))))
        assertEquals(foods.take(3).map { it.id }, result.top.map { it.food.id })
        assertTrue(result.top.none { it.unanimous })
    }

    @Test fun everyoneFollowingUsesOneRandomPickAndNoInventedVotes() {
        val players = listOf(Player("A", follows = true), Player("B", follows = true))
        val result = Matcher.calculate(foods, players, Random(42))
        assertTrue(result.top.isEmpty())
        assertTrue(result.randomFood in foods)
        assertEquals(result, Matcher.calculate(foods, players, Random(42)))
    }

    @Test fun onePlayerOneFoodWorks() {
        val rec = Matcher.calculate(foods.take(1), listOf(Player("A", mapOf(a to Vote.LIKE)))).top.single()
        assertTrue(rec.unanimous)
        assertEquals(1, rec.likes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyMenuIsRejected() { Matcher.calculate(emptyList(), listOf(Player("A"))) }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateIdsAreRejected() { Matcher.calculate(listOf(foods[0], foods[0]), listOf(Player("A"))) }

    @Test(expected = IllegalArgumentException::class)
    fun emptyPlayersAreRejected() { Matcher.calculate(foods, emptyList()) }
}
