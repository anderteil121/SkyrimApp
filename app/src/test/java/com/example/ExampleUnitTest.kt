package com.example

import com.example.data.model.*
import com.example.engine.FactionWarEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testAllFactionsDefined() {
        val factions = FactionsData.allFactions
        assertEquals(7, factions.size)
        assertTrue(factions.any { it.id == FactionId.IMPERIAL })
        assertTrue(factions.any { it.id == FactionId.STORMCLOAK })
        assertTrue(factions.any { it.id == FactionId.THALMOR })
        assertTrue(factions.any { it.id == FactionId.DAWNGUARD })
        assertTrue(factions.any { it.id == FactionId.VOLKIHAR })
        assertTrue(factions.any { it.id == FactionId.FORSWORN })
        assertTrue(factions.any { it.id == FactionId.BANDITS })
    }

    @Test
    fun testLocationsTopologyIntegrity() {
        val locations = SkyrimLocationsData.initialLocations
        assertTrue("Must have 30+ key locations", locations.size >= 30)

        val idSet = locations.map { it.id }.toSet()
        assertEquals("Location IDs must be unique", locations.size, idSet.size)

        // Verify connections point to valid location IDs
        for (loc in locations) {
            assertTrue("Location ${loc.id} must have connections", loc.connections.isNotEmpty())
            for (conn in loc.connections) {
                assertTrue("Connection $conn from ${loc.id} must exist in locations", idSet.contains(conn))
            }
        }
    }

    @Test
    fun testEngineTimeAndEconomyTick() {
        val engine = FactionWarEngine(soundManager = null)
        val initial = GameState(
            playerFactionId = FactionId.IMPERIAL,
            isGameStarted = true,
            gameDay = 1,
            gameHour = 8,
            gold = 300,
            ore = 100,
            recruits = 40
        )

        // Process tick
        val next = engine.processTick(initial)
        assertEquals(9, next.gameHour)
        assertEquals(1, next.gameDay)

        var state = initial
        repeat(5) {
            state = engine.processTick(state)
        }
        assertTrue("Gold should increase after economy cycle", state.gold > initial.gold)
    }

    @Test
    fun testSquadDispatchAndPower() {
        val engine = FactionWarEngine(soundManager = null)
        val initial = GameState(
            playerFactionId = FactionId.IMPERIAL,
            isGameStarted = true,
            gold = 500,
            ore = 300,
            recruits = 100
        )

        val stateAfterDispatch = engine.dispatchSquad(
            state = initial,
            fromLocationId = "solitude",
            targetLocationId = "dragon_bridge",
            unitClass = UnitClass.INFANTRY,
            count = 20,
            isDovahkiin = false
        )

        assertEquals(1, stateAfterDispatch.squads.size)
        val squad = stateAfterDispatch.squads.first()
        assertEquals(FactionId.IMPERIAL, squad.factionId)
        assertEquals(20, squad.count)
        assertEquals("solitude", squad.fromLocationId)
        assertEquals("dragon_bridge", squad.targetLocationId)
        assertTrue(squad.currentAttackPower(1.0f) > 0f)
    }

    @Test
    fun testDovahkiinShoutExecution() {
        val engine = FactionWarEngine(soundManager = null)
        val testBattle = Battle(
            id = "b1",
            locationId = "whiterun",
            attackerFactionId = FactionId.STORMCLOAK,
            defenderFactionId = FactionId.IMPERIAL,
            attackerStrength = 150f,
            defenderStrength = 150f,
            maxAttackerStrength = 150f,
            maxDefenderStrength = 150f
        )
        val state = GameState(
            playerFactionId = FactionId.IMPERIAL,
            isGameStarted = true,
            activeBattles = listOf(testBattle)
        )

        val stateAfterShout = engine.castShoutInBattle(
            state = state,
            battleLocationId = "whiterun",
            shoutId = ShoutId.FUS_RO_DAH
        )

        val battleAfter = stateAfterShout.activeBattles.first()
        assertTrue("Enemy attacker HP should decrease from shout", battleAfter.attackerStrength < 150f)
        assertTrue("Cooldown should be registered", (stateAfterShout.shoutCooldowns[ShoutId.FUS_RO_DAH] ?: 0) > 0)
    }

    @Test
    fun testTechProgression() {
        val engine = FactionWarEngine(soundManager = null)
        val state = GameState(
            techTier = TechTier.IRON,
            gold = 1000,
            ore = 500
        )
        val upgraded = engine.upgradeTech(state)
        assertEquals(TechTier.STEEL, upgraded.techTier)
        assertTrue(upgraded.gold < state.gold)
    }

    @Test
    fun testVictoryWhenAllLocationsCaptured() {
        val engine = FactionWarEngine(soundManager = null)
        // All locations set to player's faction (IMPERIAL)
        val allCaptured = SkyrimLocationsData.initialLocations.map {
            it.copy(ownerFactionId = FactionId.IMPERIAL)
        }
        val state = GameState(
            playerFactionId = FactionId.IMPERIAL,
            isGameStarted = true,
            locations = allCaptured,
            isGameOver = false
        )

        val tickResult = engine.processMacroTick(state)
        assertTrue("Game must trigger victory when all locations are captured", tickResult.isGameOver)
        assertEquals(GameResult.VICTORY, tickResult.gameResult)
    }

    @Test
    fun testGameDoesNotEndWhileEvenOneUncontrolledObjectRemains() {
        val engine = FactionWarEngine(soundManager = null)
        // 33 locations are IMPERIAL, but 1 location is STORMCLOAK (uncontrolled)
        val locationsWithOneUncontrolled = SkyrimLocationsData.initialLocations.mapIndexed { index, loc ->
            if (index == 0) loc.copy(ownerFactionId = FactionId.STORMCLOAK)
            else loc.copy(ownerFactionId = FactionId.IMPERIAL)
        }
        val state = GameState(
            playerFactionId = FactionId.IMPERIAL,
            isGameStarted = true,
            locations = locationsWithOneUncontrolled,
            isGameOver = false
        )

        val tickResult = engine.processMacroTick(state)
        org.junit.Assert.assertFalse(
            "Game must NOT end while there is even 1 uncontrolled object on the map",
            tickResult.isGameOver
        )
        org.junit.Assert.assertNull(tickResult.gameResult)
    }

    @Test
    fun testDefeatWhenAllLocationsLost() {
        val engine = FactionWarEngine(soundManager = null)
        // Player has 0 locations and 0 squads
        val allEnemyLocations = SkyrimLocationsData.initialLocations.map {
            it.copy(ownerFactionId = FactionId.STORMCLOAK)
        }
        val state = GameState(
            playerFactionId = FactionId.IMPERIAL,
            isGameStarted = true,
            locations = allEnemyLocations,
            squads = emptyList(),
            isGameOver = false
        )

        val tickResult = engine.processMacroTick(state)
        assertTrue("Game must trigger defeat when all locations are lost", tickResult.isGameOver)
        assertEquals(GameResult.DEFEAT, tickResult.gameResult)
    }

    @Test
    fun testSmoothMovementProgress() {
        val engine = FactionWarEngine(soundManager = null)
        val testSquad = Squad(
            id = "s1",
            factionId = FactionId.IMPERIAL,
            nameRu = "Пехота",
            unitClass = UnitClass.INFANTRY,
            count = 20,
            maxCount = 20,
            fromLocationId = "solitude",
            targetLocationId = "dragon_bridge",
            progress = 0.1f
        )
        val state = GameState(
            playerFactionId = FactionId.IMPERIAL,
            isGameStarted = true,
            squads = listOf(testSquad),
            gameSpeed = 1.0f
        )

        val updated = engine.processSmoothMovement(state, deltaTimeSeconds = 0.04f)
        val updatedSquad = updated.squads.first()
        assertTrue("Progress should increase smoothly", updatedSquad.progress > 0.1f)
        assertTrue("Progress increment should be fine-grained (< 0.05 per frame)", updatedSquad.progress < 0.15f)
    }

    @Test
    fun testMapParserExpandedPreset() {
        val parser = com.example.engine.SkyrimMapParser()
        val expanded = parser.getExpandedPresetMap()
        assertTrue("Expanded map must have more than 32 nodes", expanded.size >= 36)
        assertTrue(expanded.any { it.nameRu.contains("Лабиринтиан") })
    }

    @Test
    fun testMultipleSquadsMergeAndSumPowerAtHostileLocation() {
        val engine = FactionWarEngine(soundManager = null)
        val initialLocations = SkyrimLocationsData.initialLocations
        val targetLocId = "fort_greymoor" // Bandit controlled in initial data

        val squad1 = Squad(
            id = "s1",
            factionId = FactionId.IMPERIAL,
            nameRu = "1-я Когорта",
            unitClass = UnitClass.INFANTRY,
            count = 20,
            maxCount = 20,
            fromLocationId = "whiterun",
            targetLocationId = targetLocId,
            progress = 0.99f // Arriving immediately
        )

        var state = GameState(
            playerFactionId = FactionId.IMPERIAL,
            isGameStarted = true,
            locations = initialLocations,
            squads = listOf(squad1)
        )

        // Process arrival of squad 1 -> battle initiated
        state = engine.processSmoothMovement(state, deltaTimeSeconds = 1.0f)
        assertEquals(1, state.activeBattles.size)
        val battleAfterSquad1 = state.activeBattles.first()
        val initialAttPower = battleAfterSquad1.attackerStrength
        assertEquals(20, battleAfterSquad1.totalAttackerSoldiers)
        assertEquals(1, battleAfterSquad1.attackerSquads.size)

        // Squad 2 sent from another base (e.g. rorikstead) to the SAME fortress
        val squad2 = Squad(
            id = "s2",
            factionId = FactionId.IMPERIAL,
            nameRu = "2-я Когорта",
            unitClass = UnitClass.ARCHERS,
            count = 30,
            maxCount = 30,
            fromLocationId = "rorikstead",
            targetLocationId = targetLocId,
            progress = 0.99f // Arriving while battle is active
        )
        state = state.copy(squads = listOf(squad2))

        // Process arrival of squad 2 -> strength MUST sum up!
        state = engine.processSmoothMovement(state, deltaTimeSeconds = 1.0f)
        assertEquals(1, state.activeBattles.size)
        val battleAfterSquad2 = state.activeBattles.first()

        assertTrue("Attacker strength must be strictly greater than squad 1 alone", battleAfterSquad2.attackerStrength > initialAttPower)
        assertEquals("Total soldiers must sum up (20 + 30 = 50)", 50, battleAfterSquad2.totalAttackerSoldiers)
        assertEquals("Must track both squads", 2, battleAfterSquad2.attackerSquads.size)
        assertTrue(battleAfterSquad2.log.any { it.contains("ПОДКРЕПЛЕНИЕ") })
    }
}
