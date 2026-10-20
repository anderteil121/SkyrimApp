package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.ParseResult
import com.example.engine.SkyrimMapParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Skyrim Wars", appName)
  }

  @Test
  fun `parse GeoJSON with Android JSONObject`() {
    val parser = SkyrimMapParser()
    val sampleGeoJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "properties": { "name": "Тестовый Форт", "hold": "Вайтран", "faction": "IMPERIAL" },
              "geometry": { "type": "Point", "coordinates": [400.0, 500.0] }
            },
            {
              "type": "Feature",
              "properties": { "name": "Тестовая Шахта", "hold": "Предел", "faction": "FORSWORN" },
              "geometry": { "type": "Point", "coordinates": [200.0, 450.0] }
            }
          ]
        }
    """.trimIndent()

    val result = parser.parseJsonMap(sampleGeoJson)
    assertTrue(result is ParseResult.Success)
    val success = result as ParseResult.Success
    assertEquals(2, success.locations.size)
    assertEquals("Тестовый Форт", success.locations[0].nameRu)
    assertTrue(success.locations[0].connections.isNotEmpty())
  }

  @Test
  fun `recruitGarrison does not deduct money when location is at max garrison`() {
    val engine = com.example.engine.FactionWarEngine(soundManager = null)
    val testLoc = com.example.data.model.LocationNode(
      id = "whiterun_test",
      nameRu = "Вайтран",
      holdRu = "Вайтран",
      type = com.example.data.model.LocationType.CITY,
      x = 500f,
      y = 500f,
      ownerFactionId = com.example.data.model.FactionId.STORMCLOAK,
      garrison = 100,
      maxGarrison = 100
    )
    val state = com.example.data.model.GameState(
      playerFactionId = com.example.data.model.FactionId.STORMCLOAK,
      locations = listOf(testLoc),
      gold = 500,
      ore = 200,
      recruits = 50,
      isGameStarted = true
    )

    val updated = engine.recruitGarrison(state, "whiterun_test", 15)
    // Money, ore, and recruits must NOT change!
    assertEquals(500, updated.gold)
    assertEquals(200, updated.ore)
    assertEquals(50, updated.recruits)
    // Garrison must not exceed 100!
    assertEquals(100, updated.locations.first().garrison)
  }

  @Test
  fun `recruitGarrison only charges for slots available up to max garrison`() {
    val engine = com.example.engine.FactionWarEngine(soundManager = null)
    val testLoc = com.example.data.model.LocationNode(
      id = "whiterun_test",
      nameRu = "Вайтран",
      holdRu = "Вайтран",
      type = com.example.data.model.LocationType.CITY,
      x = 500f,
      y = 500f,
      ownerFactionId = com.example.data.model.FactionId.STORMCLOAK,
      garrison = 95,
      maxGarrison = 100
    )
    val state = com.example.data.model.GameState(
      playerFactionId = com.example.data.model.FactionId.STORMCLOAK,
      locations = listOf(testLoc),
      gold = 500,
      ore = 200,
      recruits = 50,
      isGameStarted = true
    )

    // Requesting 15 soldiers, but only 5 can fit
    val updated = engine.recruitGarrison(state, "whiterun_test", 15)
    // Charged for 5 soldiers: 5 * 6 = 30 gold, 5 * 2 = 10 ore, 5 recruits
    assertEquals(500 - 30, updated.gold)
    assertEquals(200 - 10, updated.ore)
    assertEquals(50 - 5, updated.recruits)
    // Garrison capped at 100
    assertEquals(100, updated.locations.first().garrison)
  }

  @Test
  fun `squad arrival at friendly fort caps garrison at max and refunds overflow to player`() {
    val engine = com.example.engine.FactionWarEngine(soundManager = null)
    val testLoc = com.example.data.model.LocationNode(
      id = "solitude_test",
      nameRu = "Солитьюд",
      holdRu = "Хаафингар",
      type = com.example.data.model.LocationType.CITY,
      x = 300f,
      y = 200f,
      ownerFactionId = com.example.data.model.FactionId.IMPERIAL,
      garrison = 90,
      maxGarrison = 100
    )
    val squad = com.example.data.model.Squad(
      id = "test_squad",
      factionId = com.example.data.model.FactionId.IMPERIAL,
      nameRu = "Подкрепление",
      unitClass = com.example.data.model.UnitClass.INFANTRY,
      count = 25,
      maxCount = 25,
      fromLocationId = "origin",
      targetLocationId = "solitude_test",
      progress = 0.999f,
      isPlayerSquad = true
    )
    val state = com.example.data.model.GameState(
      playerFactionId = com.example.data.model.FactionId.IMPERIAL,
      locations = listOf(testLoc),
      squads = listOf(squad),
      recruits = 30,
      isGameStarted = true
    )

    // With delta = 1.0f, progress exceeds 1.0f, squad arrives at solitude_test
    val updated = engine.processSmoothMovement(state, 1.0f)

    // Location garrison must NOT exceed maxGarrison (100)
    assertEquals(100, updated.locations.first().garrison)
    // 10 went into garrison (90 -> 100), 15 overflow refunded to player recruits (30 + 15 = 45)
    assertEquals(45, updated.recruits)
    // Squad arrived, so remaining squads list is empty
    assertTrue(updated.squads.isEmpty())
  }
}
