package com.example.data.model

enum class GameResult {
    VICTORY,
    DEFEAT
}

enum class MapVisualMode(val titleRu: String) {
    AUTHENTIC_PARCHMENT("Аутентичный пергамент Скайрима"),
    TACTICAL_SLATE("Тактический планшет (PDA)")
}

data class GameState(
    val playerFactionId: FactionId = FactionId.IMPERIAL,
    val isGameStarted: Boolean = false,
    val mapVisualMode: MapVisualMode = MapVisualMode.AUTHENTIC_PARCHMENT,
    val mapSourceDescription: String = "Встроенная карта (32 узла)",
    val gameDay: Int = 1,
    val gameHour: Int = 8,
    val isPaused: Boolean = false,
    val gameSpeed: Float = 1.0f, // 1.0, 2.0, 3.0
    val gold: Int = 300,
    val ore: Int = 120,
    val recruits: Int = 50,
    val techTier: TechTier = TechTier.IRON,
    val locations: List<LocationNode> = SkyrimLocationsData.initialLocations,
    val squads: List<Squad> = emptyList(),
    val activeBattles: List<Battle> = emptyList(),
    val feedMessages: List<FeedMessage> = listOf(
        FeedMessage(
            id = "init_1",
            gameDay = 1,
            gameHour = 8,
            type = FeedType.DIPLOMACY,
            textRu = "Гражданская война в Скайриме разгорается с новой силой. Форты и шахты готовы к мобилизации."
        ),
        FeedMessage(
            id = "init_2",
            gameDay = 1,
            gameHour = 8,
            type = FeedType.WAR_ALERT,
            textRu = "Разведка докладывает: отряды Изгоев и Бандитов активизировались на трактах Предела и Вайтрана."
        )
    ),
    val dovahkiin: DovahkiinState = DovahkiinState(),
    val selectedLocationId: String? = "solitude",
    val activeBattleModalLocationId: String? = null,
    val shoutCooldowns: Map<ShoutId, Int> = emptyMap(),
    val battleTacticsMap: Map<String, BattleTactic> = emptyMap(),
    val isGameOver: Boolean = false,
    val gameResult: GameResult? = null,
    val hasDismissedVictoryDialog: Boolean = false,
    val battlesWon: Int = 0,
    val fortsCaptured: Int = 0
) {
    val playerFaction: Faction
        get() = FactionsData.getFaction(playerFactionId)

    val selectedLocation: LocationNode?
        get() = locations.firstOrNull { it.id == selectedLocationId }

    // Faction strength stats
    fun getFactionControlledLocationsCount(factionId: FactionId): Int {
        return locations.count { it.ownerFactionId == factionId }
    }

    fun getFactionTotalGarrison(factionId: FactionId): Int {
        val locGarrisons = locations.filter { it.ownerFactionId == factionId }.sumOf { it.garrison }
        val squadCount = squads.filter { it.factionId == factionId }.sumOf { it.count }
        return locGarrisons + squadCount
    }
}
