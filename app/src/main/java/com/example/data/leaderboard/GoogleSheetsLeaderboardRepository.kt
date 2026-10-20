package com.example.data.leaderboard

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.FactionId
import com.example.data.model.GameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleSheetsLeaderboardRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("skyrim_google_sheets_leaderboard", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Default sample public Google Sheet ID (publicly readable template)
    companion object {
        const val DEFAULT_SHEET_ID = "1pRLfLrg1fvwMgXSR7SikIBn6Et1CBUankY_qwOyJjEI"
        const val DEFAULT_SHEET_NAME = "Sheet1"
        const val PREF_SHEET_ID = "sheet_id"
        const val PREF_SHEET_NAME = "sheet_name"
        const val PREF_WEBHOOK_URL = "webhook_url"
        const val PREF_CACHED_LEADERS = "cached_leaders_json"
        const val PREF_LAST_SYNC = "last_sync_timestamp"
        const val CREATE_SHEET_URL = "https://sheets.new"
    }

    var spreadsheetId: String
        get() {
            val saved = prefs.getString(PREF_SHEET_ID, null)
            return if (!saved.isNullOrBlank()) saved else DEFAULT_SHEET_ID
        }
        set(value) {
            val extracted = extractSheetId(value)
            prefs.edit().putString(PREF_SHEET_ID, extracted).apply()
        }

    var sheetName: String
        get() = prefs.getString(PREF_SHEET_NAME, DEFAULT_SHEET_NAME) ?: DEFAULT_SHEET_NAME
        set(value) {
            prefs.edit().putString(PREF_SHEET_NAME, value.trim()).apply()
        }

    var webhookUrl: String
        get() = prefs.getString(PREF_WEBHOOK_URL, "") ?: ""
        set(value) {
            prefs.edit().putString(PREF_WEBHOOK_URL, value.trim()).apply()
        }

    var lastSyncTimestamp: String
        get() = prefs.getString(PREF_LAST_SYNC, "") ?: ""
        private set(value) {
            prefs.edit().putString(PREF_LAST_SYNC, value).apply()
        }

    fun getFullSheetUrl(): String {
        val id = spreadsheetId
        return if (id.isNotBlank()) "https://docs.google.com/spreadsheets/d/$id" else ""
    }

    fun getCreateNewSheetUrl(): String = CREATE_SHEET_URL

    /**
     * Extracts pure Google Sheet ID if user pastes full URL
     * e.g. https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit#gid=0 -> 1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms
     */
    fun extractSheetId(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.contains("/spreadsheets/d/")) {
            val parts = trimmed.split("/spreadsheets/d/")
            if (parts.size > 1) {
                return parts[1].split("/").first().split("?").first().split("#").first()
            }
        }
        return trimmed
    }

    /**
     * Fetches leaderboard from Google Sheet CSV endpoint.
     * If network fails or sheet is empty, returns cached or default Skyrim champions.
     */
    suspend fun fetchLeaderboard(): Pair<List<LeaderboardEntry>, LeaderboardSourceStatus> = withContext(Dispatchers.IO) {
        val currentSheetId = spreadsheetId
        if (currentSheetId.isBlank()) {
            val cached = loadCachedEntries()
            val list = if (cached.isNotEmpty()) cached else getDefaultChampions()
            return@withContext Pair(list, LeaderboardSourceStatus.OfflineCache("Google Таблица еще не подключена"))
        }

        // Try Google Sheets GVIZ CSV endpoints:
        // 1. Direct gviz query without strict sheet name (exports the first active sheet regardless of Sheet1/Лист1)
        // 2. Query with specific sheet name if user configured one
        // 3. Direct export?format=csv
        val candidateUrls = mutableListOf<String>()
        candidateUrls.add("https://docs.google.com/spreadsheets/d/$currentSheetId/gviz/tq?tqx=out:csv")
        if (sheetName.isNotBlank() && sheetName != DEFAULT_SHEET_NAME) {
            candidateUrls.add("https://docs.google.com/spreadsheets/d/$currentSheetId/gviz/tq?tqx=out:csv&sheet=${sheetName}")
        }
        candidateUrls.add("https://docs.google.com/spreadsheets/d/$currentSheetId/export?format=csv")

        var csvContent: String? = null
        var lastError: String? = null

        for (url in candidateUrls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android) SkyrimWarApp/1.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank() && !body.contains("<!DOCTYPE html>")) {
                            csvContent = body
                            break
                        }
                    } else if (response.code == 404) {
                        lastError = "Таблица не найдена. Проверьте ID или создайте новую таблицу"
                    } else if (response.code == 401 || response.code == 403) {
                        lastError = "В Google Таблице включите: Поделиться -> Доступ: Все, у кого есть ссылка (Читатель)"
                    }
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Ошибка сети при загрузке таблицы"
            }
        }

        if (!csvContent.isNullOrBlank()) {
            val parsed = parseCsv(csvContent!!)
            val nowStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
            lastSyncTimestamp = nowStr
            if (parsed.isNotEmpty()) {
                saveCachedEntries(parsed)
                return@withContext Pair(parsed, LeaderboardSourceStatus.OnlineSuccess(currentSheetId, nowStr))
            } else {
                // Table is successfully connected and readable, but has only headers or no records yet!
                val cached = loadCachedEntries()
                return@withContext Pair(
                    cached,
                    LeaderboardSourceStatus.OnlineSuccess(currentSheetId, "Таблица подключена! Ожидает первых рекордов")
                )
            }
        }

        val cached = loadCachedEntries()
        val list = if (cached.isNotEmpty()) cached else getDefaultChampions()
        val err = lastError
        val status = if (err != null) {
            LeaderboardSourceStatus.Error(err)
        } else {
            LeaderboardSourceStatus.OfflineCache("Использован локальный кэш рекордов")
        }
        return@withContext Pair(list, status)
    }

    /**
     * Parses CSV rows safely with dynamic header recognition
     */
    private fun parseCsv(csv: String): List<LeaderboardEntry> {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val results = mutableListOf<LeaderboardEntry>()

        var nameCol = -1
        var factionCol = -1
        var scoreCol = -1
        var holdsCol = -1
        var daysCol = -1
        var outcomeCol = -1
        var dateCol = -1
        var hasDetectedHeader = false

        for ((idx, line) in lines.withIndex()) {
            val tokens = splitCsvLine(line)
            if (tokens.isEmpty()) continue

            if (idx == 0) {
                val joinedLower = tokens.joinToString(" ").lowercase()
                if (joinedLower.contains("имя") || joinedLower.contains("player") || joinedLower.contains("очки") || joinedLower.contains("score")) {
                    hasDetectedHeader = true
                    for ((cIdx, token) in tokens.withIndex()) {
                        val t = token.lowercase().trim()
                        when {
                            t.contains("имя") || t.contains("player") || t.contains("name") || t.contains("герой") -> nameCol = cIdx
                            t.contains("фракц") || t.contains("faction") || t.contains("сторона") -> factionCol = cIdx
                            t.contains("очк") || t.contains("score") || t.contains("балл") || t.contains("счет") -> scoreCol = cIdx
                            t.contains("твердын") || t.contains("влад") || t.contains("hold") || t.contains("территор") -> holdsCol = cIdx
                            t.contains("дни") || t.contains("дня") || t.contains("день") || t.contains("days") || t.contains("day") -> daysCol = cIdx
                            t.contains("итог") || t.contains("результат") || t.contains("outcome") -> outcomeCol = cIdx
                            t.contains("дата") || t.contains("date") || t.contains("время") -> dateCol = cIdx
                        }
                    }
                    continue
                }
            }

            val entry = if (hasDetectedHeader && scoreCol != -1) {
                parseMappedTokens(tokens, nameCol, factionCol, scoreCol, holdsCol, daysCol, outcomeCol, dateCol)
            } else {
                parseTokens(tokens)
            }
            if (entry != null) {
                results.add(entry)
            }
        }

        // Sort descending by score and re-rank
        return results.sortedByDescending { it.score }.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }
    }

    private fun parseMappedTokens(
        tokens: List<String>,
        nameCol: Int,
        factionCol: Int,
        scoreCol: Int,
        holdsCol: Int,
        daysCol: Int,
        outcomeCol: Int,
        dateCol: Int
    ): LeaderboardEntry? {
        val name = (if (nameCol >= 0) tokens.getOrNull(nameCol) else null)?.takeIf { it.isNotBlank() } ?: "Герой Скайрима"
        val factionRaw = if (factionCol >= 0) tokens.getOrNull(factionCol) ?: "" else ""
        val faction = parseFaction(factionRaw)
        val score = (if (scoreCol >= 0) tokens.getOrNull(scoreCol) else null)?.replace(" ", "")?.toIntOrNull() ?: 0
        val holds = if (holdsCol >= 0) tokens.getOrNull(holdsCol)?.toIntOrNull() ?: 0 else 0
        val days = if (daysCol >= 0) tokens.getOrNull(daysCol)?.toIntOrNull() ?: 1 else 1
        val outcome = (if (outcomeCol >= 0) tokens.getOrNull(outcomeCol) else null)?.takeIf { it.isNotBlank() } ?: "Победа"
        val date = (if (dateCol >= 0) tokens.getOrNull(dateCol) else null)?.takeIf { it.isNotBlank() }
            ?: SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

        if (score <= 0) return null

        return LeaderboardEntry(
            rank = 1,
            playerName = name,
            factionId = faction,
            score = score,
            holdsCaptured = holds,
            daysSurvived = days,
            outcome = outcome,
            date = date
        )
    }

    private fun splitCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '\"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim().removeSurrounding("\""))
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim().removeSurrounding("\""))
        return tokens
    }

    private fun parseTokens(tokens: List<String>): LeaderboardEntry? {
        if (tokens.size < 3) return null

        var name = ""
        var faction = FactionId.STORMCLOAK
        var score = 0
        var holds = 0
        var days = 1
        var outcome = "Победа"
        var date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

        // If first token is a rank number like "1", shift by 1
        val isFirstNumeric = tokens[0].toIntOrNull() != null
        val offset = if (isFirstNumeric && tokens.size >= 4) 1 else 0

        name = tokens.getOrNull(offset) ?: "Герой Скайрима"
        val factionRaw = tokens.getOrNull(offset + 1) ?: ""
        faction = parseFaction(factionRaw)

        score = tokens.getOrNull(offset + 2)?.replace(" ", "")?.toIntOrNull() ?: 0

        if (tokens.size - offset == 4) {
            // 4 columns: [Name, Faction, Score, Days]
            days = tokens.getOrNull(offset + 3)?.toIntOrNull() ?: 1
        } else {
            holds = tokens.getOrNull(offset + 3)?.toIntOrNull() ?: 0
            days = tokens.getOrNull(offset + 4)?.toIntOrNull() ?: 1
            outcome = tokens.getOrNull(offset + 5)?.takeIf { it.isNotBlank() } ?: "Победа"
            date = tokens.getOrNull(offset + 6)?.takeIf { it.isNotBlank() } ?: date
        }

        if (name.isBlank() || score == 0) return null

        return LeaderboardEntry(
            rank = 1,
            playerName = name,
            factionId = faction,
            score = score,
            holdsCaptured = holds,
            daysSurvived = days,
            outcome = outcome,
            date = date
        )
    }

    private fun parseFaction(raw: String): FactionId {
        val lower = raw.lowercase().trim()
        return when {
            lower.contains("бури") || lower.contains("storm") -> FactionId.STORMCLOAK
            lower.contains("импер") || lower.contains("легион") || lower.contains("imper") -> FactionId.IMPERIAL
            lower.contains("талмор") || lower.contains("thalmor") -> FactionId.THALMOR
            lower.contains("рассвет") || lower.contains("dawn") -> FactionId.DAWNGUARD
            lower.contains("волкихар") || lower.contains("вампир") || lower.contains("volk") -> FactionId.VOLKIHAR
            lower.contains("изго") || lower.contains("forsworn") -> FactionId.FORSWORN
            lower.contains("бандит") || lower.contains("bandit") -> FactionId.BANDITS
            else -> FactionId.STORMCLOAK
        }
    }

    /**
     * Submits player score:
     * 1) If Google Apps Script Webhook URL is configured, sends HTTP POST
     * 2) Saves to local cache so it appears immediately in the player's rankings
     */
    suspend fun submitScore(entry: LeaderboardEntry): Result<String> = withContext(Dispatchers.IO) {
        // Save to local cache first
        val currentList = loadCachedEntries().toMutableList()
        currentList.removeAll { it.playerName == entry.playerName && it.date == entry.date }
        currentList.add(entry.copy(isPlayer = true))
        val updatedSorted = currentList.sortedByDescending { it.score }.mapIndexed { index, item ->
            item.copy(rank = index + 1)
        }
        saveCachedEntries(updatedSorted)

        val targetWebhook = webhookUrl
        if (targetWebhook.isNotBlank()) {
            try {
                val json = JSONObject().apply {
                    put("playerName", entry.playerName)
                    put("faction", entry.factionName)
                    put("score", entry.score)
                    put("holdsCaptured", entry.holdsCaptured)
                    put("daysSurvived", entry.daysSurvived)
                    put("outcome", entry.outcome)
                    put("date", entry.date)
                }

                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val req = Request.Builder()
                    .url(targetWebhook)
                    .post(body)
                    .build()

                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext Result.success("Успешно отправлено в Google Таблицу через веб-хук!")
                    } else {
                        return@withContext Result.success("Сохранено локально (Веб-хук ответил кодом ${response.code})")
                    }
                }
            } catch (e: Exception) {
                return@withContext Result.success("Сохранено локально в рейтинг (Ошибка веб-хука: ${e.localizedMessage})")
            }
        }

        return@withContext Result.success("Результат сохранен в Зале Славы и скопирован в буфер! Откройте таблицу и нажмите Вставить.")
    }

    /**
     * Generates a ready TSV line formatted for copy-pasting directly into Google Sheets (matches columns: Имя, Фракция, Очки, Дни)
     */
    fun formatCsvRowForClipboard(entry: LeaderboardEntry): String {
        return "${entry.playerName}\t${entry.factionName}\t${entry.score}\t${entry.daysSurvived}"
    }

    fun formatCsvRowFullForClipboard(entry: LeaderboardEntry): String {
        return "${entry.playerName}\t${entry.factionName}\t${entry.score}\t${entry.holdsCaptured}\t${entry.daysSurvived}\t${entry.outcome}\t${entry.date}"
    }

    fun formatHeaderForClipboard(): String {
        return "Имя\tФракция\tОчки\tДни"
    }

    fun formatHeaderAndRowForClipboard(entry: LeaderboardEntry): String {
        return "${formatHeaderForClipboard()}\n${formatCsvRowForClipboard(entry)}"
    }

    /**
     * Calculates campaign score for GameState
     */
    fun calculateScore(gameState: GameState): Int {
        val playerFaction = gameState.playerFactionId
        val holds = gameState.getFactionControlledLocationsCount(playerFaction)
        val battlesWon = gameState.battlesWon
        val fortsCaptured = gameState.fortsCaptured
        val gold = gameState.gold
        val days = gameState.gameDay
        val shoutsLearned = gameState.dovahkiin.unlockedShouts.size
        val techTierIndex = gameState.techTier.ordinal + 1

        val baseScore = (holds * 1200) +
                (battlesWon * 350) +
                (fortsCaptured * 800) +
                (gold / 2) +
                (days * 45) +
                (shoutsLearned * 600) +
                (techTierIndex * 750)

        val multiplier = if (gameState.gameResult == com.example.data.model.GameResult.VICTORY) 1.5f else 1.0f
        return (baseScore * multiplier).toInt()
    }

    private fun saveCachedEntries(entries: List<LeaderboardEntry>) {
        val arr = JSONArray()
        for (e in entries) {
            val obj = JSONObject().apply {
                put("rank", e.rank)
                put("name", e.playerName)
                put("faction", e.factionId.name)
                put("score", e.score)
                put("holds", e.holdsCaptured)
                put("days", e.daysSurvived)
                put("outcome", e.outcome)
                put("date", e.date)
                put("isPlayer", e.isPlayer)
            }
            arr.put(obj)
        }
        prefs.edit().putString(PREF_CACHED_LEADERS, arr.toString()).apply()
    }

    fun loadCachedEntries(): List<LeaderboardEntry> {
        val json = prefs.getString(PREF_CACHED_LEADERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<LeaderboardEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val factionName = obj.optString("faction", FactionId.STORMCLOAK.name)
                val faction = try {
                    FactionId.valueOf(factionName)
                } catch (e: Exception) {
                    FactionId.STORMCLOAK
                }
                list.add(
                    LeaderboardEntry(
                        rank = obj.optInt("rank", i + 1),
                        playerName = obj.optString("name", "Герой"),
                        factionId = faction,
                        score = obj.optInt("score", 0),
                        holdsCaptured = obj.optInt("holds", 0),
                        daysSurvived = obj.optInt("days", 1),
                        outcome = obj.optString("outcome", "Победа"),
                        date = obj.optString("date", ""),
                        isPlayer = obj.optBoolean("isPlayer", false)
                    )
                )
            }
            list.sortedByDescending { it.score }.mapIndexed { index, item -> item.copy(rank = index + 1) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getDefaultChampions(): List<LeaderboardEntry> {
        return listOf(
            LeaderboardEntry(
                rank = 1,
                playerName = "Довакин (Драконорожденный)",
                factionId = FactionId.STORMCLOAK,
                score = 26500,
                holdsCaptured = 16,
                daysSurvived = 35,
                outcome = "Победа (Легенда)",
                date = "14.10.2026"
            ),
            LeaderboardEntry(
                rank = 2,
                playerName = "Генерал Туллий",
                factionId = FactionId.IMPERIAL,
                score = 22400,
                holdsCaptured = 15,
                daysSurvived = 29,
                outcome = "Победа (Мастер)",
                date = "12.10.2026"
            ),
            LeaderboardEntry(
                rank = 3,
                playerName = "Ульфрик Буревестник",
                factionId = FactionId.STORMCLOAK,
                score = 20800,
                holdsCaptured = 14,
                daysSurvived = 26,
                outcome = "Победа",
                date = "10.10.2026"
            ),
            LeaderboardEntry(
                rank = 4,
                playerName = "Лорд Харкон",
                factionId = FactionId.VOLKIHAR,
                score = 17650,
                holdsCaptured = 11,
                daysSurvived = 34,
                outcome = "Победа",
                date = "07.10.2026"
            ),
            LeaderboardEntry(
                rank = 5,
                playerName = "Исран (Охотник на вампиров)",
                factionId = FactionId.DAWNGUARD,
                score = 16200,
                holdsCaptured = 9,
                daysSurvived = 23,
                outcome = "Победа",
                date = "04.10.2026"
            ),
            LeaderboardEntry(
                rank = 6,
                playerName = "Первый Эмиссар Эленвен",
                factionId = FactionId.THALMOR,
                score = 14100,
                holdsCaptured = 8,
                daysSurvived = 19,
                outcome = "Поражение",
                date = "01.10.2026"
            ),
            LeaderboardEntry(
                rank = 7,
                playerName = "Маданах (Король в Лохмотьях)",
                factionId = FactionId.FORSWORN,
                score = 11950,
                holdsCaptured = 6,
                daysSurvived = 16,
                outcome = "Поражение",
                date = "27.09.2026"
            ),
            LeaderboardEntry(
                rank = 8,
                playerName = "Главарь Черного Брода",
                factionId = FactionId.BANDITS,
                score = 8600,
                holdsCaptured = 4,
                daysSurvived = 12,
                outcome = "Поражение",
                date = "22.09.2026"
            )
        )
    }
}
