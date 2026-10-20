package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.leaderboard.GoogleSheetsLeaderboardRepository
import com.example.data.leaderboard.LeaderboardEntry
import com.example.data.leaderboard.LeaderboardSourceStatus
import com.example.data.model.*
import com.example.engine.FactionWarEngine
import com.example.engine.SoundManager
import com.example.storage.GameSaveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val soundManager = SoundManager(application)
    private val saveManager = GameSaveManager(application)
    private val engine = FactionWarEngine(soundManager)
    val leaderboardRepository = GoogleSheetsLeaderboardRepository(application)

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries.asStateFlow()

    private val _leaderboardStatus = MutableStateFlow<LeaderboardSourceStatus>(LeaderboardSourceStatus.Loading)
    val leaderboardStatus: StateFlow<LeaderboardSourceStatus> = _leaderboardStatus.asStateFlow()

    private val _isSubmittingScore = MutableStateFlow(false)
    val isSubmittingScore: StateFlow<Boolean> = _isSubmittingScore.asStateFlow()

    private var simulationJob: Job? = null
    val hasSavedGame: Boolean
        get() = saveManager.hasSave()

    init {
        val saved = saveManager.loadGame()
        if (saved != null) {
            _gameState.value = saved
            startTicker()
        }
        refreshLeaderboard()
    }

    private fun startTicker() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            var macroAccumulatorMs = 0L
            var saveCounter = 0
            val frameDelayMs = 16L // ~60 FPS for ultra-smooth buttery squad motion

            while (isActive) {
                delay(frameDelayMs)

                val currentState = _gameState.value
                if (!currentState.isPaused && currentState.isGameStarted && (!currentState.isGameOver || currentState.hasDismissedVictoryDialog)) {
                    val deltaSeconds = (frameDelayMs / 1000f)

                    // 1. Smooth squad movement at ~60 FPS ONLY when squads are actively marching
                    if (currentState.squads.isNotEmpty()) {
                        _gameState.update { current ->
                            engine.processSmoothMovement(current, deltaSeconds)
                        }
                    }

                    // 2. Macro tick (battles, economy, AI decisions, clock) every ~1.2s adjusted by gameSpeed
                    val macroThreshold = (1200L / currentState.gameSpeed).toLong().coerceIn(300L, 2000L)
                    macroAccumulatorMs += frameDelayMs

                    if (macroAccumulatorMs >= macroThreshold) {
                        macroAccumulatorMs = 0L
                        _gameState.update { current ->
                            engine.processMacroTick(current)
                        }

                        saveCounter++
                        if (saveCounter >= 10) {
                            saveCounter = 0
                            val snapshot = _gameState.value
                            launch(Dispatchers.IO) {
                                saveManager.saveGame(snapshot)
                            }
                        }
                    }
                }
            }
        }
    }

    fun startNewCampaign(factionId: FactionId) {
        saveManager.clearSave()
        val baseLocs = SkyrimLocationsData.initialLocations
        val startLoc = baseLocs.firstOrNull { it.ownerFactionId == factionId } ?: baseLocs.first()

        _gameState.value = GameState(
            playerFactionId = factionId,
            isGameStarted = true,
            selectedLocationId = startLoc.id,
            dovahkiin = DovahkiinState(currentLocationId = startLoc.id),
            gold = 350 + FactionsData.getFaction(factionId).baseGoldPerTurn * 2,
            ore = 150 + FactionsData.getFaction(factionId).baseOrePerTurn * 2,
            recruits = 60 + FactionsData.getFaction(factionId).baseRecruitsPerTurn * 2,
            isGameOver = false,
            gameResult = null,
            hasDismissedVictoryDialog = false
        )
        soundManager.playWarHorn()
        startTicker()
    }

    fun resumeCampaign() {
        val saved = saveManager.loadGame()
        if (saved != null) {
            _gameState.value = saved
            startTicker()
        }
    }

    fun continueSandbox() {
        _gameState.update { it.copy(hasDismissedVictoryDialog = true, isPaused = false) }
    }

    fun finishGameEarly() {
        _gameState.update { current ->
            current.copy(
                isGameOver = true,
                gameResult = GameResult.DEFEAT,
                hasDismissedVictoryDialog = false,
                isPaused = true
            )
        }
    }

    fun abandonCampaign() {
        saveManager.clearSave()
        _gameState.update { it.copy(isGameStarted = false, isPaused = true) }
    }

    fun exitToMainMenu(saveProgress: Boolean = true) {
        if (saveProgress) {
            saveManager.saveGame(_gameState.value)
        } else {
            saveManager.clearSave()
        }
        _gameState.update { it.copy(isGameStarted = false) }
    }

    fun togglePause() {
        _gameState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun setGameSpeed(speed: Float) {
        _gameState.update { it.copy(gameSpeed = speed) }
    }

    fun selectLocation(locationId: String) {
        _gameState.update { it.copy(selectedLocationId = locationId) }
    }

    fun recruitGarrison(locationId: String, count: Int) {
        _gameState.update { current ->
            engine.recruitGarrison(current, locationId, count)
        }
    }

    fun dispatchSquad(
        fromLocationId: String,
        targetLocationId: String,
        unitClass: UnitClass,
        count: Int,
        isDovahkiin: Boolean = false
    ) {
        _gameState.update { current ->
            engine.dispatchSquad(current, fromLocationId, targetLocationId, unitClass, count, isDovahkiin)
        }
    }

    // Direct squads from multiple bases to 1 enemy fortress simultaneously
    fun dispatchMassAssault(
        fromLocationIds: List<String>,
        targetLocationId: String,
        unitClass: UnitClass = UnitClass.INFANTRY,
        countPerBase: Int = 20
    ) {
        _gameState.update { current ->
            engine.dispatchMassAssault(current, fromLocationIds, targetLocationId, unitClass, countPerBase)
        }
    }

    fun upgradeFortification(locationId: String) {
        _gameState.update { current ->
            engine.upgradeFortification(current, locationId)
        }
    }

    fun upgradeTechTier() {
        _gameState.update { current ->
            engine.upgradeTech(current)
        }
    }

    fun openBattleModal(locationId: String) {
        val current = _gameState.value
        val battle = current.activeBattles.firstOrNull { it.locationId == locationId }
        // The player can only observe and command battles their faction is involved in!
        if (battle != null && battle.isPlayerInvolved(current.playerFactionId)) {
            _gameState.update { it.copy(activeBattleModalLocationId = locationId) }
        }
    }

    fun closeBattleModal() {
        _gameState.update { it.copy(activeBattleModalLocationId = null) }
    }

    fun setBattleTactic(locationId: String, tactic: BattleTactic) {
        _gameState.update { current ->
            current.copy(
                battleTacticsMap = current.battleTacticsMap + (locationId to tactic)
            )
        }
    }

    fun castShout(shoutId: ShoutId) {
        val activeBattleLoc = _gameState.value.activeBattleModalLocationId
            ?: _gameState.value.activeBattles.firstOrNull()?.locationId
            ?: return

        _gameState.update { current ->
            engine.castShoutInBattle(current, activeBattleLoc, shoutId)
        }
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            _leaderboardStatus.value = LeaderboardSourceStatus.Loading
            val (entries, status) = leaderboardRepository.fetchLeaderboard()
            _leaderboardEntries.value = entries
            _leaderboardStatus.value = status
        }
    }

    fun saveLeaderboardSettings(sheetIdOrUrl: String, sheetName: String, webhookUrl: String) {
        leaderboardRepository.spreadsheetId = sheetIdOrUrl
        leaderboardRepository.sheetName = sheetName
        leaderboardRepository.webhookUrl = webhookUrl
        refreshLeaderboard()
    }

    fun calculateCurrentScore(): Int {
        return leaderboardRepository.calculateScore(_gameState.value)
    }

    fun getCreateNewSheetUrl(): String = leaderboardRepository.getCreateNewSheetUrl()

    fun getFullSheetUrl(): String = leaderboardRepository.getFullSheetUrl()

    fun getTemplateHeader(): String = leaderboardRepository.formatHeaderForClipboard()

    fun getClipboardCsvRow(playerName: String): String {
        val entry = createCurrentEntry(playerName)
        return leaderboardRepository.formatCsvRowForClipboard(entry)
    }

    fun getClipboardHeaderAndRow(playerName: String): String {
        val entry = createCurrentEntry(playerName)
        return leaderboardRepository.formatHeaderAndRowForClipboard(entry)
    }

    private fun createCurrentEntry(playerName: String): LeaderboardEntry {
        val state = _gameState.value
        val score = calculateCurrentScore()
        val holds = state.getFactionControlledLocationsCount(state.playerFactionId)
        val days = state.gameDay
        val outcome = when (state.gameResult) {
            GameResult.VICTORY -> "Победа"
            GameResult.DEFEAT -> "Поражение"
            null -> if (holds >= state.locations.size) "Победа (Песочница)" else "Кампания"
        }
        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

        return LeaderboardEntry(
            rank = 1,
            playerName = playerName.trim().substringBefore("@").ifBlank { "Довакин" },
            factionId = state.playerFactionId,
            score = score,
            holdsCaptured = holds,
            daysSurvived = days,
            outcome = outcome,
            date = date,
            isPlayer = true
        )
    }

    fun submitCurrentScore(playerName: String, onFinished: (String) -> Unit) {
        viewModelScope.launch {
            _isSubmittingScore.value = true
            val state = _gameState.value
            val score = calculateCurrentScore()
            val holds = state.getFactionControlledLocationsCount(state.playerFactionId)
            val days = state.gameDay
            val outcome = when (state.gameResult) {
                GameResult.VICTORY -> "Победа"
                GameResult.DEFEAT -> "Поражение"
                null -> if (holds >= state.locations.size) "Победа (Песочница)" else "Кампания"
            }
            val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

            val entry = LeaderboardEntry(
                rank = 1,
                playerName = playerName.trim().substringBefore("@").ifBlank { "Довакин" },
                factionId = state.playerFactionId,
                score = score,
                holdsCaptured = holds,
                daysSurvived = days,
                outcome = outcome,
                date = date,
                isPlayer = true
            )

            val result = leaderboardRepository.submitScore(entry)
            _isSubmittingScore.value = false

            // Refresh entries to show local change
            val cached = leaderboardRepository.loadCachedEntries()
            if (cached.isNotEmpty()) {
                _leaderboardEntries.value = cached
            }

            onFinished(result.getOrDefault("Результат записан!"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        saveManager.saveGame(_gameState.value)
    }
}
