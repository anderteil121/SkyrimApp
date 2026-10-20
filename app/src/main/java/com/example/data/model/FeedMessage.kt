package com.example.data.model

enum class FeedType {
    WAR_ALERT,      // Assaults, sieges
    CONQUEST,       // Fort / city taken
    AMBUSH,         // Skirmishes, caravan raids
    REINFORCEMENTS, // New squads deployed
    DIPLOMACY       // General news & rumors
}

data class FeedMessage(
    val id: String,
    val gameDay: Int,
    val gameHour: Int,
    val type: FeedType,
    val textRu: String,
    val factionId: FactionId? = null,
    val locationId: String? = null
) {
    val formattedTime: String
        get() = "День $gameDay, %02d:00".format(gameHour)
}
