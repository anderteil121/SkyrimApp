package com.example.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject

class GameSaveManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("skyrim_war_save", Context.MODE_PRIVATE)

    fun hasSave(): Boolean = prefs.getBoolean("has_saved_game", false)

    fun saveGame(state: GameState) {
        val editor = prefs.edit()
        editor.putBoolean("has_saved_game", true)
        editor.putString("player_faction", state.playerFactionId.name)
        editor.putInt("game_day", state.gameDay)
        editor.putInt("game_hour", state.gameHour)
        editor.putInt("gold", state.gold)
        editor.putInt("ore", state.ore)
        editor.putInt("recruits", state.recruits)
        editor.putString("tech_tier", state.techTier.name)
        editor.putString("dovahkiin_loc", state.dovahkiin.currentLocationId)

        // Locations state as JSON
        val locArray = JSONArray()
        for (loc in state.locations) {
            val obj = JSONObject().apply {
                put("id", loc.id)
                put("owner", loc.ownerFactionId.name)
                put("garrison", loc.garrison)
                put("fortLevel", loc.fortificationLevel)
                put("siege", loc.isUnderSiege)
            }
            locArray.put(obj)
        }
        editor.putString("locations_json", locArray.toString())
        editor.apply()
    }

    fun loadGame(): GameState? {
        if (!hasSave()) return null
        return try {
            val factionName = prefs.getString("player_faction", FactionId.IMPERIAL.name) ?: FactionId.IMPERIAL.name
            val playerFaction = FactionId.valueOf(factionName)
            val gameDay = prefs.getInt("game_day", 1)
            val gameHour = prefs.getInt("game_hour", 8)
            val gold = prefs.getInt("gold", 300)
            val ore = prefs.getInt("ore", 120)
            val recruits = prefs.getInt("recruits", 50)
            val techTierName = prefs.getString("tech_tier", TechTier.IRON.name) ?: TechTier.IRON.name
            val techTier = TechTier.valueOf(techTierName)
            val dovahkiinLoc = prefs.getString("dovahkiin_loc", "whiterun") ?: "whiterun"

            val locsJson = prefs.getString("locations_json", null)
            val loadedLocations = if (locsJson != null) {
                val locArray = JSONArray(locsJson)
                val locMap = mutableMapOf<String, Triple<FactionId, Int, Int>>()
                for (i in 0 until locArray.length()) {
                    val obj = locArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val owner = FactionId.valueOf(obj.getString("owner"))
                    val garrison = obj.getInt("garrison")
                    val fortLevel = obj.getInt("fortLevel")
                    locMap[id] = Triple(owner, garrison, fortLevel)
                }
                SkyrimLocationsData.initialLocations.map { base ->
                    val saved = locMap[base.id]
                    if (saved != null) {
                        base.copy(
                            ownerFactionId = saved.first,
                            garrison = saved.second.coerceAtMost(base.maxGarrison),
                            fortificationLevel = saved.third
                        )
                    } else base
                }
            } else {
                SkyrimLocationsData.initialLocations
            }

            GameState(
                playerFactionId = playerFaction,
                isGameStarted = true,
                gameDay = gameDay,
                gameHour = gameHour,
                gold = gold,
                ore = ore,
                recruits = recruits,
                techTier = techTier,
                locations = loadedLocations,
                dovahkiin = DovahkiinState(currentLocationId = dovahkiinLoc)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearSave() {
        prefs.edit().clear().apply()
    }
}
