package com.example.engine

import com.example.data.model.*
import java.util.UUID
import kotlin.random.Random

class FactionWarEngine(
    private val soundManager: SoundManager? = null
) {
    // High-frequency smooth squad movement loop (runs at ~60 FPS)
    fun processSmoothMovement(current: GameState, deltaTimeSeconds: Float): GameState {
        if (current.isPaused || !current.isGameStarted) return current
        if (current.squads.isEmpty()) return current

        // Fast-path: check if any squad arrived at destination (avoids allocations on 99.9% of frames)
        var anyArrived = false
        val precalcSteps = FloatArray(current.squads.size)
        for (i in current.squads.indices) {
            val sq = current.squads[i]
            val step = (0.055f * sq.unitClass.speed * current.gameSpeed * deltaTimeSeconds).coerceIn(0.0005f, 0.25f)
            precalcSteps[i] = step
            if (sq.progress + step >= 1.0f) {
                anyArrived = true
            }
        }

        if (!anyArrived) {
            val movedSquads = ArrayList<Squad>(current.squads.size)
            for (i in current.squads.indices) {
                movedSquads.add(current.squads[i].copy(progress = current.squads[i].progress + precalcSteps[i]))
            }
            return current.copy(squads = movedSquads)
        }

        var updated = current
        val remainingSquads = ArrayList<Squad>(current.squads.size)
        var currentLocations = updated.locations
        val newFeed = updated.feedMessages.toMutableList()
        val currentBattles = updated.activeBattles.toMutableList()
        var fortsCapturedCount = updated.fortsCaptured
        var battlesWonCount = updated.battlesWon
        var returnedPlayerRecruits = 0

        for (i in updated.squads.indices) {
            val squad = updated.squads[i]
            val newProgress = squad.progress + precalcSteps[i]

            if (newProgress >= 1.0f) {
                // Squad arrived at destination!
                val targetLoc = currentLocations.firstOrNull { it.id == squad.targetLocationId }
                if (targetLoc != null) {
                    val existingBattle = currentBattles.firstOrNull { it.locationId == targetLoc.id }
                    val squadPower = squad.currentAttackPower(1.0f)
                    val squadInfo = ParticipatingSquadInfo(
                        nameRu = squad.nameRu,
                        count = squad.count,
                        unitClass = squad.unitClass,
                        isDovahkiin = squad.isDovahkiinSquad,
                        power = squadPower
                    )

                    if (targetLoc.ownerFactionId == squad.factionId && existingBattle == null) {
                        // 1. Friendly fortress with NO siege -> Add directly to garrison (strictly respecting maxGarrison limit!)
                        val availableSlots = (targetLoc.maxGarrison - targetLoc.garrison).coerceAtLeast(0)
                        val fitIntoGarrison = squad.count.coerceAtMost(availableSlots)
                        val overflow = squad.count - fitIntoGarrison
                        val newGarrison = (targetLoc.garrison + fitIntoGarrison).coerceAtMost(targetLoc.maxGarrison)

                        currentLocations = currentLocations.map {
                            if (it.id == targetLoc.id) it.copy(garrison = newGarrison) else it
                        }

                        if (squad.isPlayerSquad) {
                            if (overflow > 0) {
                                returnedPlayerRecruits += overflow
                            }
                            val overflowNote = if (overflow > 0) " (излишек в $overflow бойцов вернулся в резерв)" else ""
                            newFeed.add(
                                0,
                                FeedMessage(
                                    id = UUID.randomUUID().toString(),
                                    gameDay = updated.gameDay,
                                    gameHour = updated.gameHour,
                                    type = FeedType.REINFORCEMENTS,
                                    textRu = "ОБЪЕДИНЕНИЕ СИЛ: Отряд '${squad.nameRu}' (+${fitIntoGarrison} воинов) вошел в ${targetLoc.nameRu}. Гарнизон: $newGarrison/${targetLoc.maxGarrison}$overflowNote.",
                                    factionId = squad.factionId,
                                    locationId = targetLoc.id
                                )
                            )
                            soundManager?.playGoldCoins()
                        }
                    } else if (existingBattle != null) {
                        // 2. Battle is ALREADY IN PROGRESS -> SUM UP AND POOL SQUAD STRENGTH!
                        if (existingBattle.attackerFactionId == squad.factionId) {
                            // Arrived as ATTACKER reinforcement: add strength and soldier count!
                            val newAttStrength = existingBattle.attackerStrength + squadPower
                            val newMaxAttStrength = existingBattle.maxAttackerStrength + squadPower
                            val newTotalAttSoldiers = existingBattle.totalAttackerSoldiers + squad.count
                            val updatedBattle = existingBattle.copy(
                                attackerStrength = newAttStrength,
                                maxAttackerStrength = newMaxAttStrength,
                                totalAttackerSoldiers = newTotalAttSoldiers,
                                attackerSquads = existingBattle.attackerSquads + squadInfo,
                                isDovahkiinPresent = existingBattle.isDovahkiinPresent || squad.isDovahkiinSquad,
                                log = existingBattle.log + "⚔️ ПОДКРЕПЛЕНИЕ: Отряд '${squad.nameRu}' (+${squad.count} бойцов, +${squadPower.toInt()} мощи) соединился с осадной армией! Общая мощь: ${newAttStrength.toInt()} HP."
                            )
                            val bIndex = currentBattles.indexOfFirst { it.id == existingBattle.id }
                            if (bIndex >= 0) {
                                currentBattles[bIndex] = updatedBattle
                            }

                            newFeed.add(
                                0,
                                FeedMessage(
                                    id = UUID.randomUUID().toString(),
                                    gameDay = updated.gameDay,
                                    gameHour = updated.gameHour,
                                    type = FeedType.REINFORCEMENTS,
                                    textRu = "ОБЪЕДИНЕНИЕ СИЛ: Отряд '${squad.nameRu}' (+${squad.count} воинов) прибыл из базы на штурм ${targetLoc.nameRu}! Силы армий суммированы (${updatedBattle.attackerSquads.size} отрядов).",
                                    factionId = squad.factionId,
                                    locationId = targetLoc.id
                                )
                            )
                            soundManager?.playWarHorn()

                        } else if (existingBattle.defenderFactionId == squad.factionId) {
                            // Arrived as DEFENDER reinforcement: add defense strength and garrison!
                            val newDefStrength = existingBattle.defenderStrength + squadPower
                            val newMaxDefStrength = existingBattle.maxDefenderStrength + squadPower
                            val newTotalDefSoldiers = existingBattle.totalDefenderSoldiers + squad.count
                            val updatedBattle = existingBattle.copy(
                                defenderStrength = newDefStrength,
                                maxDefenderStrength = newMaxDefStrength,
                                totalDefenderSoldiers = newTotalDefSoldiers,
                                defenderSquads = existingBattle.defenderSquads + squadInfo,
                                isDovahkiinPresent = existingBattle.isDovahkiinPresent || squad.isDovahkiinSquad,
                                log = existingBattle.log + "🛡️ ПОДКРЕПЛЕНИЕ ГАРНИЗОНА: Отряд '${squad.nameRu}' (+${squad.count} защитников, +${squadPower.toInt()} стойкости) пробился в форт!"
                            )
                            val bIndex = currentBattles.indexOfFirst { it.id == existingBattle.id }
                            if (bIndex >= 0) {
                                currentBattles[bIndex] = updatedBattle
                            }

                            newFeed.add(
                                0,
                                FeedMessage(
                                    id = UUID.randomUUID().toString(),
                                    gameDay = updated.gameDay,
                                    gameHour = updated.gameHour,
                                    type = FeedType.REINFORCEMENTS,
                                    textRu = "ОБОРОНА: Подкрепление '${squad.nameRu}' (${squad.count} воинов) прорвало блокаду и вошло в гарнизон ${targetLoc.nameRu}!",
                                    factionId = squad.factionId,
                                    locationId = targetLoc.id
                                )
                            )
                            soundManager?.playWarHorn()

                        } else {
                            // Third faction arrives: joins assault on defender
                            val newAttStrength = existingBattle.attackerStrength + squadPower
                            val newTotalAttSoldiers = existingBattle.totalAttackerSoldiers + squad.count
                            val updatedBattle = existingBattle.copy(
                                attackerStrength = newAttStrength,
                                maxAttackerStrength = existingBattle.maxAttackerStrength + squadPower,
                                totalAttackerSoldiers = newTotalAttSoldiers,
                                attackerSquads = existingBattle.attackerSquads + squadInfo,
                                log = existingBattle.log + "ТРЕТЬЯ СТОРОНА: '${squad.nameRu}' присоединился к штурму!"
                            )
                            val bIndex = currentBattles.indexOfFirst { it.id == existingBattle.id }
                            if (bIndex >= 0) {
                                currentBattles[bIndex] = updatedBattle
                            }
                        }
                    } else {
                        // 3. No existing battle & hostile location -> Start NEW siege!
                        val defPower = (targetLoc.garrison * 1.5f * (1f + targetLoc.fortificationLevel * 0.2f)).coerceAtLeast(15f)
                        val newBattle = Battle(
                            id = UUID.randomUUID().toString(),
                            locationId = targetLoc.id,
                            attackerFactionId = squad.factionId,
                            defenderFactionId = targetLoc.ownerFactionId,
                            attackerStrength = squadPower,
                            defenderStrength = defPower,
                            maxAttackerStrength = squadPower,
                            maxDefenderStrength = defPower,
                            isDovahkiinPresent = squad.isDovahkiinSquad,
                            attackerSquads = listOf(squadInfo),
                            defenderSquads = listOf(
                                ParticipatingSquadInfo(
                                    nameRu = "Гарнизон цитадели",
                                    count = targetLoc.garrison,
                                    unitClass = UnitClass.INFANTRY,
                                    power = defPower
                                )
                            ),
                            totalAttackerSoldiers = squad.count,
                            totalDefenderSoldiers = targetLoc.garrison,
                            log = listOf("⚔️ Отряд '${squad.nameRu}' (${squad.count} бойцов) начал осаду ${targetLoc.nameRu}!")
                        )
                        currentBattles.add(newBattle)

                        currentLocations = currentLocations.map {
                            if (it.id == targetLoc.id) it.copy(isUnderSiege = true) else it
                        }

                        val attFaction = FactionsData.getFaction(squad.factionId)
                        val defFaction = FactionsData.getFaction(targetLoc.ownerFactionId)
                        newFeed.add(
                            0,
                            FeedMessage(
                                id = UUID.randomUUID().toString(),
                                gameDay = updated.gameDay,
                                gameHour = updated.gameHour,
                                type = FeedType.WAR_ALERT,
                                textRu = "ШТУРМ: ${attFaction.nameRu} осаждает ${targetLoc.nameRu} (${defFaction.nameRu})!",
                                factionId = squad.factionId,
                                locationId = targetLoc.id
                            )
                        )
                        soundManager?.playWarHorn()
                    }
                }
            } else {
                remainingSquads.add(squad.copy(progress = newProgress))
            }
        }

        updated = updated.copy(
            squads = remainingSquads,
            locations = currentLocations,
            activeBattles = currentBattles,
            feedMessages = newFeed.take(60),
            fortsCaptured = fortsCapturedCount,
            battlesWon = battlesWonCount,
            recruits = updated.recruits + returnedPlayerRecruits
        )

        return checkVictoryConditions(updated)
    }

    // Macro logic ticker (clock, cooldowns, battles, economy, AI strategy)
    fun processMacroTick(current: GameState): GameState {
        if (current.isPaused || !current.isGameStarted) return current

        var updated = current

        // 1. Advance Time
        var newHour = updated.gameHour + 1
        var newDay = updated.gameDay
        if (newHour >= 24) {
            newHour = 0
            newDay += 1
        }
        updated = updated.copy(gameHour = newHour, gameDay = newDay)

        // 2. Reduce Cooldowns
        val newCooldowns = updated.shoutCooldowns.mapValues { (_, remaining) ->
            (remaining - 1).coerceAtLeast(0)
        }.filterValues { it > 0 }
        updated = updated.copy(shoutCooldowns = newCooldowns)

        // 3. Economy (every 4 hours)
        if (newHour % 4 == 0) {
            updated = processEconomy(updated)
        }

        // 4. Resolve Battles (with damage scaling proportionally to army strength)
        updated = processBattles(updated)

        // 5. AI Faction Decisions
        if (Random.nextFloat() < 0.35f * updated.gameSpeed) {
            updated = processAiDecisions(updated)
        }

        return checkVictoryConditions(updated)
    }

    // Unified single tick for unit tests or standard ticker
    fun processTick(current: GameState): GameState {
        if (current.isPaused || !current.isGameStarted) return current
        val updated = processSmoothMovement(current, deltaTimeSeconds = 1.0f)
        return processMacroTick(updated)
    }

    fun checkVictoryConditions(state: GameState): GameState {
        if (state.isGameOver) return state

        // Requirement: Game must NOT end while there is even a single uncontrolled object!
        // Victory triggers ONLY when 100% of all locations in Skyrim are under player faction's control,
        // and all sieges/battles are resolved.
        val uncapturedLocations = state.locations.filter { it.ownerFactionId != state.playerFactionId }
        val hasUncontrolledObjects = uncapturedLocations.isNotEmpty()

        val allOwnedByPlayer = !hasUncontrolledObjects && state.locations.isNotEmpty()
        val allBattlesResolved = state.activeBattles.isEmpty() && state.locations.none { it.isUnderSiege }

        if (allOwnedByPlayer && allBattlesResolved) {
            val newFeed = state.feedMessages.toMutableList()
            newFeed.add(
                0,
                FeedMessage(
                    id = UUID.randomUUID().toString(),
                    gameDay = state.gameDay,
                    gameHour = state.gameHour,
                    type = FeedType.CONQUEST,
                    textRu = "ВЕЛИКИЙ ТРИУМФ СКАЙРИМА: Все 100% твердынь, городов, шахт и оплотов провинции под вашим контролем! Ни одного неподконтрольного объекта не осталось!",
                    factionId = state.playerFactionId
                )
            )
            soundManager?.playConquest()
            return state.copy(
                isGameOver = true,
                gameResult = GameResult.VICTORY,
                activeBattles = emptyList(),
                squads = state.squads.filter { it.factionId == state.playerFactionId },
                feedMessages = newFeed.take(60)
            )
        }

        // Check if player lost all locations and has no active squads
        val playerLostAll = state.locations.none { it.ownerFactionId == state.playerFactionId } &&
                state.squads.none { it.factionId == state.playerFactionId }
        if (playerLostAll) {
            val newFeed = state.feedMessages.toMutableList()
            newFeed.add(
                0,
                FeedMessage(
                    id = UUID.randomUUID().toString(),
                    gameDay = state.gameDay,
                    gameHour = state.gameHour,
                    type = FeedType.WAR_ALERT,
                    textRu = "ПОРАЖЕНИЕ: Ваша фракция потеряла последний оплот в провинции. Знамена повержены.",
                    factionId = state.playerFactionId
                )
            )
            return state.copy(
                isGameOver = true,
                gameResult = GameResult.DEFEAT,
                feedMessages = newFeed.take(60)
            )
        }

        return state
    }

    private fun processEconomy(state: GameState): GameState {
        var addedGold = state.playerFaction.baseGoldPerTurn
        var addedOre = state.playerFaction.baseOrePerTurn
        var addedRecruits = state.playerFaction.baseRecruitsPerTurn

        val updatedLocations = state.locations.map { loc ->
            if (loc.ownerFactionId == state.playerFactionId) {
                addedGold += loc.goldIncome
                addedOre += loc.oreIncome
                addedRecruits += loc.recruitIncome
            }
            if (!loc.isUnderSiege && loc.garrison < loc.maxGarrison) {
                val growth = if (loc.type == LocationType.CITY) 3 else 2
                loc.copy(garrison = (loc.garrison + growth).coerceAtMost(loc.maxGarrison))
            } else {
                loc
            }
        }

        return state.copy(
            gold = state.gold + addedGold,
            ore = state.ore + addedOre,
            recruits = state.recruits + addedRecruits,
            locations = updatedLocations
        )
    }

    private fun processBattles(state: GameState): GameState {
        if (state.activeBattles.isEmpty()) return state

        var currentLocations = state.locations
        val remainingBattles = mutableListOf<Battle>()
        val newFeed = state.feedMessages.toMutableList()
        var newBattlesWon = state.battlesWon
        var newFortsCaptured = state.fortsCaptured

        for (battle in state.activeBattles) {
            val loc = currentLocations.firstOrNull { it.id == battle.locationId }
            if (loc == null) continue

            val tactic = state.battleTacticsMap[battle.locationId] ?: battle.selectedTactic

            val (attMod, defMod) = when (tactic) {
                BattleTactic.SHIELD_WALL -> Pair(0.8f, 1.4f)
                BattleTactic.ARCHER_VOLLEY -> Pair(1.3f, 0.9f)
                BattleTactic.FLANK_ATTACK -> Pair(1.5f, 0.8f)
                BattleTactic.MAGIC_SURGE -> Pair(1.6f, 0.7f)
                BattleTactic.ALL_OUT_CHARGE -> Pair(1.8f, 0.6f)
                BattleTactic.RETREAT -> Pair(0.3f, 1.2f)
                BattleTactic.BALANCED -> Pair(1.0f, 1.0f)
            }

            val dovahkiinBonus = if (battle.isDovahkiinPresent) 1.35f else 1.0f

            // Combat damage scales with combined army strength so reinforced squads actually hit hard!
            val attPowerFactor = (battle.attackerStrength / 35f).coerceIn(0.6f, 5.0f)
            val defPowerFactor = (battle.defenderStrength / 35f).coerceIn(0.6f, 4.0f)

            val baseAttDmg = (3.5f + attPowerFactor * 3.0f + Random.nextFloat() * 2.5f) * attMod * dovahkiinBonus * state.gameSpeed
            val baseDefDmg = (3.0f + defPowerFactor * 2.5f + Random.nextFloat() * 2.0f) * defMod * (1f + loc.fortificationLevel * 0.12f) * state.gameSpeed

            val newDefStrength = (battle.defenderStrength - baseAttDmg).coerceAtLeast(0f)
            val newAttStrength = (battle.attackerStrength - baseDefDmg).coerceAtLeast(0f)

            soundManager?.playSwordClash()

            val attFaction = FactionsData.getFaction(battle.attackerFactionId)
            val defFaction = FactionsData.getFaction(battle.defenderFactionId)

            if (newDefStrength <= 0f) {
                // ATTACKER WINS! All participating squads contribute to surviving garrison!
                val healthRatio = (newAttStrength / battle.maxAttackerStrength.coerceAtLeast(1f)).coerceIn(0.25f, 1f)
                val baseSoldiers = if (battle.totalAttackerSoldiers > 0) battle.totalAttackerSoldiers else 25
                val survivors = (baseSoldiers * healthRatio).toInt().coerceIn(15, loc.maxGarrison)

                currentLocations = currentLocations.map {
                    if (it.id == loc.id) {
                        it.copy(
                            ownerFactionId = battle.attackerFactionId,
                            garrison = survivors,
                            isUnderSiege = false,
                            fortificationLevel = (it.fortificationLevel - 1).coerceAtLeast(1)
                        )
                    } else it
                }

                val squadCountNote = if (battle.attackerSquads.size > 1) "объединенной армией из ${battle.attackerSquads.size} отрядов" else "армией"

                newFeed.add(
                    0,
                    FeedMessage(
                        id = UUID.randomUUID().toString(),
                        gameDay = state.gameDay,
                        gameHour = state.gameHour,
                        type = FeedType.CONQUEST,
                        textRu = "ЗАХВАТ: ${loc.nameRu} пал перед $squadCountNote '${attFaction.nameRu}'! В живых осталось $survivors воинов.",
                        factionId = battle.attackerFactionId,
                        locationId = loc.id
                    )
                )

                if (battle.attackerFactionId == state.playerFactionId) {
                    newFortsCaptured++
                    newBattlesWon++
                    soundManager?.playConquest()
                }

            } else if (newAttStrength <= 0f || tactic == BattleTactic.RETREAT) {
                // DEFENDER HOLDS
                val healthRatio = (newDefStrength / battle.maxDefenderStrength.coerceAtLeast(1f)).coerceIn(0.25f, 1f)
                val baseSoldiers = if (battle.totalDefenderSoldiers > 0) battle.totalDefenderSoldiers else loc.garrison
                val survivors = (baseSoldiers * healthRatio).toInt().coerceIn(10, loc.maxGarrison)

                currentLocations = currentLocations.map {
                    if (it.id == loc.id) it.copy(garrison = survivors, isUnderSiege = false) else it
                }

                newFeed.add(
                    0,
                    FeedMessage(
                        id = UUID.randomUUID().toString(),
                        gameDay = state.gameDay,
                        gameHour = state.gameHour,
                        type = FeedType.AMBUSH,
                        textRu = "ОБОРОНА: ${loc.nameRu} выдержал штурм! Нападающие разбиты. Гарнизон: $survivors воинов.",
                        factionId = battle.defenderFactionId,
                        locationId = loc.id
                    )
                )

                if (battle.defenderFactionId == state.playerFactionId) {
                    newBattlesWon++
                    soundManager?.playConquest()
                }

            } else {
                val roundSummary = "Раунд боя: Оборона: ${(newDefStrength).toInt()} HP (${battle.totalDefenderSoldiers} воинов), Атака: ${(newAttStrength).toInt()} HP (${battle.totalAttackerSoldiers} воинов из ${battle.attackerSquads.size} отр.)"
                val updatedBattle = battle.copy(
                    attackerStrength = newAttStrength,
                    defenderStrength = newDefStrength,
                    selectedTactic = tactic,
                    log = (battle.log + roundSummary).takeLast(10)
                )
                remainingBattles.add(updatedBattle)
            }
        }

        return state.copy(
            locations = currentLocations,
            activeBattles = remainingBattles,
            feedMessages = newFeed.take(60),
            battlesWon = newBattlesWon,
            fortsCaptured = newFortsCaptured
        )
    }

    private fun processAiDecisions(state: GameState): GameState {
        val aiFactions = FactionId.entries.filter { it != state.playerFactionId }
        val chosenAi = aiFactions.randomOrNull() ?: return state

        val aiControlledLocations = state.locations.filter { it.ownerFactionId == chosenAi && !it.isUnderSiege }
        if (aiControlledLocations.isEmpty()) return state

        val source = aiControlledLocations.filter { it.garrison > 35 }.randomOrNull() ?: return state

        val potentialTargets = source.connections.mapNotNull { connId ->
            state.locations.firstOrNull { it.id == connId }
        }

        val hostileTargets = potentialTargets.filter { it.ownerFactionId != chosenAi }
        val target = hostileTargets.randomOrNull() ?: return state

        val unitClass = when {
            chosenAi == FactionId.THALMOR -> UnitClass.MAGES
            chosenAi == FactionId.DAWNGUARD -> UnitClass.ARCHERS
            chosenAi == FactionId.FORSWORN -> UnitClass.RECON
            chosenAi == FactionId.BANDITS -> UnitClass.RECON
            else -> listOf(UnitClass.INFANTRY, UnitClass.ARCHERS, UnitClass.ELITE).random()
        }

        val sendCount = (source.garrison * 0.45f).toInt().coerceIn(12, 40)
        val newSourceGarrison = source.garrison - sendCount

        val squad = Squad(
            id = UUID.randomUUID().toString(),
            factionId = chosenAi,
            nameRu = "Карательный отряд ${FactionsData.getFaction(chosenAi).nameRu}",
            unitClass = unitClass,
            count = sendCount,
            maxCount = sendCount,
            fromLocationId = source.id,
            targetLocationId = target.id,
            progress = 0f,
            isPlayerSquad = false
        )

        val updatedLocations = state.locations.map {
            if (it.id == source.id) it.copy(garrison = newSourceGarrison) else it
        }

        val newFeed = state.feedMessages.toMutableList()
        val aiFaction = FactionsData.getFaction(chosenAi)
        newFeed.add(
            0,
            FeedMessage(
                id = UUID.randomUUID().toString(),
                gameDay = state.gameDay,
                gameHour = state.gameHour,
                type = FeedType.WAR_ALERT,
                textRu = "РАЗВЕДКА: Отряд ${aiFaction.nameRu} выступил из ${source.nameRu} на ${target.nameRu}!",
                factionId = chosenAi,
                locationId = target.id
            )
        )

        return state.copy(
            locations = updatedLocations,
            squads = state.squads + squad,
            feedMessages = newFeed.take(60)
        )
    }

    // Player Actions
    fun recruitGarrison(state: GameState, locationId: String, count: Int): GameState {
        val loc = state.locations.firstOrNull { it.id == locationId } ?: return state
        if (loc.ownerFactionId != state.playerFactionId) return state

        // Strictly respect maximum garrison limit
        val availableCapacity = (loc.maxGarrison - loc.garrison).coerceAtLeast(0)
        if (availableCapacity <= 0) {
            // Point is already at maximum capacity! Money and resources must NOT be deducted.
            return state
        }

        // Only recruit as many as can fit within maxGarrison
        val actualCount = count.coerceAtMost(availableCapacity)
        if (actualCount <= 0) return state

        val costGold = actualCount * 6
        val costOre = actualCount * 2
        val costRecruits = actualCount

        if (state.gold < costGold || state.ore < costOre || state.recruits < costRecruits) return state

        soundManager?.playGoldCoins()

        val updatedLocations = state.locations.map {
            if (it.id == locationId) it.copy(garrison = (it.garrison + actualCount).coerceAtMost(it.maxGarrison)) else it
        }

        return state.copy(
            gold = state.gold - costGold,
            ore = state.ore - costOre,
            recruits = state.recruits - costRecruits,
            locations = updatedLocations
        )
    }

    fun dispatchSquad(
        state: GameState,
        fromLocationId: String,
        targetLocationId: String,
        unitClass: UnitClass,
        count: Int,
        isDovahkiin: Boolean = false
    ): GameState {
        val source = state.locations.firstOrNull { it.id == fromLocationId } ?: return state
        if (source.ownerFactionId != state.playerFactionId) return state

        val costGold = unitClass.costGold * (count / 10).coerceAtLeast(1)
        val costOre = unitClass.costOre * (count / 10).coerceAtLeast(1)
        val costRecruits = count

        if (state.gold < costGold || state.ore < costOre || state.recruits < costRecruits) return state

        soundManager?.playWarHorn()

        val fromName = source.nameRu.split(" ").first()
        val squadName = if (isDovahkiin) "Флагман Драконорожденного ($fromName)" else "${unitClass.titleRu} из $fromName"

        val squad = Squad(
            id = UUID.randomUUID().toString(),
            factionId = state.playerFactionId,
            nameRu = squadName,
            unitClass = unitClass,
            count = count,
            maxCount = count,
            fromLocationId = fromLocationId,
            targetLocationId = targetLocationId,
            progress = 0f,
            isPlayerSquad = true,
            isDovahkiinSquad = isDovahkiin
        )

        val updatedDovahkiin = if (isDovahkiin) {
            state.dovahkiin.copy(currentLocationId = targetLocationId)
        } else state.dovahkiin

        return state.copy(
            gold = state.gold - costGold,
            ore = state.ore - costOre,
            recruits = state.recruits - costRecruits,
            squads = state.squads + squad,
            dovahkiin = updatedDovahkiin
        )
    }

    // Mass assault: dispatches squads from multiple bases to 1 target fortress simultaneously
    fun dispatchMassAssault(
        state: GameState,
        fromLocationIds: List<String>,
        targetLocationId: String,
        unitClass: UnitClass = UnitClass.INFANTRY,
        countPerBase: Int = 20
    ): GameState {
        var current = state
        var dispatchedCount = 0

        for (fromId in fromLocationIds) {
            val source = current.locations.firstOrNull { it.id == fromId } ?: continue
            if (source.ownerFactionId != current.playerFactionId) continue

            val costGold = unitClass.costGold * (countPerBase / 10).coerceAtLeast(1)
            val costOre = unitClass.costOre * (countPerBase / 10).coerceAtLeast(1)
            val costRecruits = countPerBase

            if (current.gold >= costGold && current.ore >= costOre && current.recruits >= costRecruits) {
                current = dispatchSquad(
                    state = current,
                    fromLocationId = fromId,
                    targetLocationId = targetLocationId,
                    unitClass = unitClass,
                    count = countPerBase,
                    isDovahkiin = false
                )
                dispatchedCount++
            }
        }

        if (dispatchedCount > 0) {
            val target = state.locations.firstOrNull { it.id == targetLocationId }
            val newFeed = current.feedMessages.toMutableList()
            newFeed.add(
                0,
                FeedMessage(
                    id = UUID.randomUUID().toString(),
                    gameDay = current.gameDay,
                    gameHour = current.gameHour,
                    type = FeedType.WAR_ALERT,
                    textRu = "⚔️ МАССИРОВАННЫЙ ШТУРМ: Направлено $dispatchedCount отрядов из разных баз на ${target?.nameRu ?: "цитадель"}! Силы соединятся при осаде.",
                    factionId = current.playerFactionId,
                    locationId = targetLocationId
                )
            )
            current = current.copy(feedMessages = newFeed.take(60))
        }

        return current
    }

    fun upgradeFortification(state: GameState, locationId: String): GameState {
        val loc = state.locations.firstOrNull { it.id == locationId } ?: return state
        if (loc.ownerFactionId != state.playerFactionId || loc.fortificationLevel >= 5) return state

        val costGold = loc.fortificationLevel * 160
        val costOre = loc.fortificationLevel * 90

        if (state.gold < costGold || state.ore < costOre) return state

        soundManager?.playGoldCoins()

        val updatedLocations = state.locations.map {
            if (it.id == locationId) it.copy(fortificationLevel = it.fortificationLevel + 1) else it
        }

        return state.copy(
            gold = state.gold - costGold,
            ore = state.ore - costOre,
            locations = updatedLocations
        )
    }

    fun upgradeTech(state: GameState): GameState {
        val nextTier = state.techTier.nextTier() ?: return state
        if (state.gold < nextTier.upgradeCostGold || state.ore < nextTier.upgradeCostOre) return state

        soundManager?.playConquest()

        val newFeed = state.feedMessages.toMutableList()
        newFeed.add(
            0,
            FeedMessage(
                id = UUID.randomUUID().toString(),
                gameDay = state.gameDay,
                gameHour = state.gameHour,
                type = FeedType.REINFORCEMENTS,
                textRu = "КУЗНИЦА: Открыты технологии '${nextTier.titleRu}'! Сила всех войск увеличена на 20%.",
                factionId = state.playerFactionId
            )
        )

        return state.copy(
            gold = state.gold - nextTier.upgradeCostGold,
            ore = state.ore - nextTier.upgradeCostOre,
            techTier = nextTier,
            feedMessages = newFeed.take(60)
        )
    }

    fun castShoutInBattle(state: GameState, battleLocationId: String, shoutId: ShoutId): GameState {
        val battle = state.activeBattles.firstOrNull { it.locationId == battleLocationId } ?: return state
        val cooldown = state.shoutCooldowns[shoutId] ?: 0
        if (cooldown > 0) return state

        soundManager?.playShout(shoutId)

        val damageBonus = when (shoutId) {
            ShoutId.FUS_RO_DAH -> 35f
            ShoutId.YOL_TOOR_SHUL -> 50f
            ShoutId.TIID_KLO_UL -> 25f
            ShoutId.ODAHVIING -> 70f
        }

        // Shout blasts the enemy forces
        val isPlayerAttacker = battle.attackerFactionId == state.playerFactionId
        val newDefStr = if (isPlayerAttacker) (battle.defenderStrength - damageBonus).coerceAtLeast(0f) else battle.defenderStrength
        val newAttStr = if (!isPlayerAttacker) (battle.attackerStrength - damageBonus).coerceAtLeast(0f) else battle.attackerStrength

        val updatedBattle = battle.copy(
            defenderStrength = newDefStr,
            attackerStrength = newAttStr,
            log = battle.log + "ДОВАКИН: Клич '${shoutId.words}' сотряс ряды врага! Нанесено ${damageBonus.toInt()} урона!"
        )

        val updatedBattles = state.activeBattles.map {
            if (it.id == battle.id) updatedBattle else it
        }

        val updatedCooldowns = state.shoutCooldowns + (shoutId to shoutId.cooldownSeconds)

        return state.copy(
            activeBattles = updatedBattles,
            shoutCooldowns = updatedCooldowns
        )
    }
}
