package com.example.data.leaderboard

import com.example.data.model.FactionId
import com.example.data.model.FactionsData

data class LeaderboardEntry(
    val rank: Int = 1,
    val playerName: String,
    val factionId: FactionId,
    val score: Int,
    val holdsCaptured: Int,
    val daysSurvived: Int,
    val outcome: String,
    val date: String,
    val isPlayer: Boolean = false
) {
    val factionName: String
        get() = FactionsData.getFaction(factionId).nameRu
}

sealed class LeaderboardSourceStatus {
    data object Loading : LeaderboardSourceStatus()
    data class OnlineSuccess(val sheetId: String, val lastSyncTime: String) : LeaderboardSourceStatus()
    data class OfflineCache(val message: String) : LeaderboardSourceStatus()
    data class Error(val message: String) : LeaderboardSourceStatus()
}
