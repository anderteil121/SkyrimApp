package com.example.engine

import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.hypot

sealed class ParseResult {
    data class Success(val locations: List<LocationNode>, val sourceName: String) : ParseResult()
    data class Error(val message: String) : ParseResult()
}

class SkyrimMapParser {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun parseFromUrl(url: String): ParseResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SkyrimFactionWar/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ParseResult.Error("HTTP ошибка ${response.code}: ${response.message}")
                }
                val bodyString = response.body?.string() ?: return@withContext ParseResult.Error("Пустой ответ от сервера")
                parseJsonMap(bodyString, sourceName = url)
            }
        } catch (e: Exception) {
            ParseResult.Error("Не удалось загрузить данные карты: ${e.localizedMessage ?: e.message}")
        }
    }

    fun parseJsonMap(jsonString: String, sourceName: String = "Пользовательский источник"): ParseResult {
        return try {
            val parsedLocations = mutableListOf<LocationNode>()
            val root = JSONObject(jsonString)

            if (root.has("features")) {
                // GeoJSON format
                val features = root.getJSONArray("features")
                for (i in 0 until features.length()) {
                    val feat = features.getJSONObject(i)
                    val props = feat.optJSONObject("properties") ?: JSONObject()
                    val geom = feat.optJSONObject("geometry") ?: JSONObject()
                    val coords = geom.optJSONArray("coordinates") ?: JSONArray()

                    val name = props.optString("name", props.optString("title", "Локация $i"))
                    val hold = props.optString("hold", "Скайрим")
                    val typeStr = props.optString("type", "FORT").uppercase()
                    val type = when {
                        typeStr.contains("CITY") -> LocationType.CITY
                        typeStr.contains("MINE") -> LocationType.MINE
                        typeStr.contains("SETTLE") -> LocationType.SETTLEMENT
                        else -> LocationType.FORT
                    }

                    var x = 500f
                    var y = 500f
                    if (coords.length() >= 2) {
                        x = coords.getDouble(0).toFloat()
                        y = coords.getDouble(1).toFloat()
                    }

                    val ownerStr = props.optString("faction", "BANDITS").uppercase()
                    val owner = when {
                        ownerStr.contains("IMP") -> FactionId.IMPERIAL
                        ownerStr.contains("STORM") -> FactionId.STORMCLOAK
                        ownerStr.contains("THAL") -> FactionId.THALMOR
                        ownerStr.contains("DAWN") -> FactionId.DAWNGUARD
                        ownerStr.contains("VOLK") -> FactionId.VOLKIHAR
                        ownerStr.contains("FORS") -> FactionId.FORSWORN
                        else -> FactionId.BANDITS
                    }

                    parsedLocations.add(
                        LocationNode(
                            id = "parsed_${i}_${name.lowercase().replace(" ", "_")}",
                            nameRu = name,
                            holdRu = hold,
                            type = type,
                            x = x.coerceIn(50f, 950f),
                            y = y.coerceIn(50f, 950f),
                            ownerFactionId = owner,
                            connections = emptyList(), // Connected automatically below
                            garrison = props.optInt("garrison", 40),
                            maxGarrison = props.optInt("maxGarrison", 80),
                            fortificationLevel = props.optInt("fortification", 2),
                            goldIncome = props.optInt("gold", 25),
                            oreIncome = props.optInt("ore", 15),
                            recruitIncome = props.optInt("recruits", 10)
                        )
                    )
                }
            } else if (root.has("locations")) {
                // Direct JSON Array
                val locs = root.getJSONArray("locations")
                for (i in 0 until locs.length()) {
                    val obj = locs.getJSONObject(i)
                    val id = obj.optString("id", "loc_$i")
                    val name = obj.optString("name", "Точка $i")
                    val hold = obj.optString("hold", "Скайрим")
                    val x = obj.optDouble("x", 500.0).toFloat()
                    val y = obj.optDouble("y", 500.0).toFloat()
                    val owner = FactionId.valueOf(obj.optString("owner", "IMPERIAL"))
                    parsedLocations.add(
                        LocationNode(
                            id = id,
                            nameRu = name,
                            holdRu = hold,
                            type = LocationType.FORT,
                            x = x.coerceIn(50f, 950f),
                            y = y.coerceIn(50f, 950f),
                            ownerFactionId = owner,
                            connections = emptyList(),
                            garrison = obj.optInt("garrison", 45),
                            maxGarrison = 90,
                            goldIncome = 25,
                            oreIncome = 15,
                            recruitIncome = 10
                        )
                    )
                }
            }

            if (parsedLocations.isEmpty()) {
                return ParseResult.Error("Не найдено точек в предоставленном JSON (ожидались features или locations)")
            }

            // Auto-connect nodes via graph proximity so roads exist!
            val connectedLocations = connectProximityNodes(parsedLocations)
            ParseResult.Success(connectedLocations, sourceName)
        } catch (e: Exception) {
            ParseResult.Error("Ошибка синтаксического анализа: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun connectProximityNodes(nodes: List<LocationNode>): List<LocationNode> {
        return nodes.map { node ->
            // Connect to 2..4 closest neighbor nodes
            val nearest = nodes
                .filter { it.id != node.id }
                .sortedBy { hypot(it.x - node.x, it.y - node.y) }
                .take(3)
                .map { it.id }
            node.copy(connections = nearest)
        }
    }

    // Extended 38-node Skyrim War Map Preset with additional shrines & mountain passes
    fun getExpandedPresetMap(): List<LocationNode> {
        val base = SkyrimLocationsData.initialLocations
        val extraNodes = listOf(
            LocationNode(
                id = "lab_sanctuary",
                nameRu = "Лабиринтиан (Руины Бромьунара)",
                holdRu = "Хьялмарк",
                type = LocationType.STRONGHOLD,
                x = 420f, y = 390f,
                ownerFactionId = FactionId.BANDITS,
                connections = listOf("morthal", "whiterun", "silent_moons"),
                garrison = 45, maxGarrison = 85, fortificationLevel = 4,
                goldIncome = 30, oreIncome = 20, recruitIncome = 10
            ),
            LocationNode(
                id = "pale_pass",
                nameRu = "Белый Проход (Южная граница)",
                holdRu = "Фолкрит",
                type = LocationType.FORT,
                x = 600f, y = 850f,
                ownerFactionId = FactionId.IMPERIAL,
                connections = listOf("fort_neugrad", "trevas_watch"),
                garrison = 50, maxGarrison = 95, fortificationLevel = 3,
                goldIncome = 25, oreIncome = 20, recruitIncome = 12
            ),
            LocationNode(
                id = "red_eagle_redoubt",
                nameRu = "Оплот Красного Орла",
                holdRu = "Предел",
                type = LocationType.STRONGHOLD,
                x = 110f, y = 490f,
                ownerFactionId = FactionId.FORSWORN,
                connections = listOf("druadach_redoubt", "kolskeggr_mine"),
                garrison = 40, maxGarrison = 80, fortificationLevel = 3,
                goldIncome = 20, oreIncome = 35, recruitIncome = 14
            ),
            LocationNode(
                id = "blackreach_lift",
                nameRu = "Подъемник в Черный Предел",
                holdRu = "Белый Берег",
                type = LocationType.MINE,
                x = 610f, y = 350f,
                ownerFactionId = FactionId.THALMOR,
                connections = listOf("fort_dunstad", "windhelm"),
                garrison = 35, maxGarrison = 70, fortificationLevel = 2,
                goldIncome = 50, oreIncome = 60, recruitIncome = 8
            )
        )
        return base + extraNodes
    }
}
