package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GameViewModel
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: GameViewModel = viewModel()
                val gameState by viewModel.gameState.collectAsStateWithLifecycle()
                val leaderboardEntries by viewModel.leaderboardEntries.collectAsStateWithLifecycle()
                val leaderboardStatus by viewModel.leaderboardStatus.collectAsStateWithLifecycle()
                val isSubmittingScore by viewModel.isSubmittingScore.collectAsStateWithLifecycle()

                var showWarFeed by remember { mutableStateOf(false) }
                var showTechTree by remember { mutableStateOf(false) }
                var showShoutsDialog by remember { mutableStateOf(false) }
                var showLeaderboard by remember { mutableStateOf(false) }
                var showExitMenuDialog by remember { mutableStateOf(false) }

                // Unified Back button behavior
                BackHandler {
                    when {
                        showLeaderboard -> showLeaderboard = false
                        showExitMenuDialog -> showExitMenuDialog = false
                        !gameState.isGameStarted -> finish()
                        gameState.activeBattleModalLocationId != null -> viewModel.closeBattleModal()
                        showWarFeed -> showWarFeed = false
                        showTechTree -> showTechTree = false
                        showShoutsDialog -> showShoutsDialog = false
                        gameState.selectedLocationId != null -> viewModel.selectLocation("")
                        else -> showExitMenuDialog = true
                    }
                }

                if (!gameState.isGameStarted) {
                    FactionSelectScreen(
                        hasSavedGame = viewModel.hasSavedGame,
                        onResumeGame = { viewModel.resumeCampaign() },
                        onStartNewGame = { factionId -> viewModel.startNewCampaign(factionId) },
                        onOpenLeaderboard = { showLeaderboard = true }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            val maxSheetHeight = maxHeight * 0.72f

                            // Interactive Map View with high-performance smooth 60 FPS gestures
                            SkyrimMapView(
                                gameState = gameState,
                                onLocationSelected = { locId -> viewModel.selectLocation(locId) },
                                onOpenBattle = { locId -> viewModel.openBattleModal(locId) },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Top HUD Bar
                            TopFactionHud(
                                gameState = gameState,
                                onTogglePause = { viewModel.togglePause() },
                                onSetSpeed = { speed -> viewModel.setGameSpeed(speed) },
                                onOpenWarFeed = { showWarFeed = true },
                                onOpenTechTree = { showTechTree = true },
                                onOpenShouts = { showShoutsDialog = true },
                                onOpenLeaderboard = { showLeaderboard = true },
                                onOpenExitMenu = { showExitMenuDialog = true },
                                modifier = Modifier.align(Alignment.TopCenter)
                            )

                            // Bottom Selected Location Action Sheet with smooth slide & fade animation
                            val selectedLoc = gameState.selectedLocation
                            AnimatedVisibility(
                                visible = selectedLoc != null,
                                enter = slideInVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
                                exit = slideOutVertically(animationSpec = tween(180)) { it } + fadeOut(),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                if (selectedLoc != null) {
                                    LocationActionSheet(
                                        gameState = gameState,
                                        location = selectedLoc,
                                        maxHeight = maxSheetHeight,
                                        onRecruitGarrison = { locId, count -> viewModel.recruitGarrison(locId, count) },
                                        onUpgradeFortification = { locId -> viewModel.upgradeFortification(locId) },
                                        onDispatchSquad = { fromId, targetId, unitClass, count, isDovahkiin ->
                                             viewModel.dispatchSquad(fromId, targetId, unitClass, count, isDovahkiin)
                                        },
                                        onDispatchMassAssault = { fromIds, targetId, unitClass, count ->
                                            viewModel.dispatchMassAssault(fromIds, targetId, unitClass, count)
                                        },
                                        onOpenBattle = { locId -> viewModel.openBattleModal(locId) },
                                        onClose = { viewModel.selectLocation("") }
                                    )
                                }
                            }

                            // S.T.A.L.K.E.R. PDA War Feed Drawer with smooth horizontal slide
                            AnimatedVisibility(
                                visible = showWarFeed,
                                enter = slideInHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
                                exit = slideOutHorizontally(animationSpec = tween(180)) { it } + fadeOut(),
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                WarFeedDrawer(
                                    feedMessages = gameState.feedMessages,
                                    playerFactionId = gameState.playerFactionId,
                                    onClose = { showWarFeed = false }
                                )
                            }

                            // Tech Tree Forge Modal
                            if (showTechTree) {
                                TechTreeDialog(
                                    gameState = gameState,
                                    onUpgradeTech = { viewModel.upgradeTechTier() },
                                    onClose = { showTechTree = false }
                                )
                            }

                            // Dovahkiin Shouts Modal
                            if (showShoutsDialog) {
                                DovahkiinSkillDialog(
                                    gameState = gameState,
                                    onMoveDovahkiin = { targetId ->
                                        viewModel.dispatchSquad(
                                            fromLocationId = gameState.dovahkiin.currentLocationId,
                                            targetLocationId = targetId,
                                            unitClass = com.example.data.model.UnitClass.DOVAHKIIN_GUARD,
                                            count = 20,
                                            isDovahkiin = true
                                        )
                                        showShoutsDialog = false
                                    },
                                    onClose = { showShoutsDialog = false }
                                )
                            }

                            // Battle Tactical Modal (only available for player's battles!)
                            val battleLocId = gameState.activeBattleModalLocationId
                            if (battleLocId != null) {
                                val battle = gameState.activeBattles.firstOrNull { it.locationId == battleLocId }
                                val loc = gameState.locations.firstOrNull { it.id == battleLocId }
                                if (battle != null && loc != null && battle.isPlayerInvolved(gameState.playerFactionId)) {
                                    BattleModal(
                                        battle = battle,
                                        location = loc,
                                        gameState = gameState,
                                        onSelectTactic = { tactic -> viewModel.setBattleTactic(battleLocId, tactic) },
                                        onCastShout = { shoutId -> viewModel.castShout(shoutId) },
                                        onClose = { viewModel.closeBattleModal() }
                                    )
                                } else {
                                    viewModel.closeBattleModal()
                                }
                            }

                            // Campaign Early Exit / Menu Dialog
                            if (showExitMenuDialog) {
                                EarlyExitDialog(
                                    gameState = gameState,
                                    onDismiss = { showExitMenuDialog = false },
                                    onFinishEarly = {
                                        showExitMenuDialog = false
                                        viewModel.finishGameEarly()
                                    },
                                    onSaveAndExit = {
                                        showExitMenuDialog = false
                                        viewModel.exitToMainMenu(saveProgress = true)
                                    },
                                    onAbandonAndExit = {
                                        showExitMenuDialog = false
                                        viewModel.abandonCampaign()
                                    }
                                )
                            }

                            // Campaign Victory / Defeat Modal (When all points captured or lost, or concluded early)
                            if (gameState.isGameOver && !gameState.hasDismissedVictoryDialog) {
                                GameOverDialog(
                                    gameState = gameState,
                                    onContinueSandbox = { viewModel.continueSandbox() },
                                    onRestartNewGame = { viewModel.exitToMainMenu() },
                                    onOpenLeaderboard = { showLeaderboard = true }
                                )
                            }
                        }
                    }
                }

                // Global Leaderboard Dialog (Google Sheets)
                if (showLeaderboard) {
                    LeaderboardDialog(
                        entries = leaderboardEntries,
                        status = leaderboardStatus,
                        currentSheetId = viewModel.leaderboardRepository.spreadsheetId,
                        currentSheetName = viewModel.leaderboardRepository.sheetName,
                        currentWebhookUrl = viewModel.leaderboardRepository.webhookUrl,
                        gameState = if (gameState.isGameStarted) gameState else null,
                        calculatedScore = viewModel.calculateCurrentScore(),
                        isSubmitting = isSubmittingScore,
                        onRefresh = { viewModel.refreshLeaderboard() },
                        onSaveSettings = { sheetId, sheetName, webhook ->
                            viewModel.saveLeaderboardSettings(sheetId, sheetName, webhook)
                        },
                        onSubmitScore = { playerName ->
                            viewModel.submitCurrentScore(playerName) { msg ->
                                android.widget.Toast.makeText(this@MainActivity, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onGetClipboardRow = { playerName ->
                            viewModel.getClipboardCsvRow(playerName)
                        },
                        onGetClipboardHeaderAndRow = { playerName ->
                            viewModel.getClipboardHeaderAndRow(playerName)
                        },
                        onClose = { showLeaderboard = false }
                    )
                }
            }
        }
    }
}
