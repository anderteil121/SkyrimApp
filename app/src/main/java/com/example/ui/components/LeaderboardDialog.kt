package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.leaderboard.LeaderboardEntry
import com.example.data.leaderboard.LeaderboardSourceStatus
import com.example.data.model.FactionId
import com.example.data.model.FactionsData
import com.example.data.model.GameState
import com.example.ui.theme.*

@Composable
fun LeaderboardDialog(
    entries: List<LeaderboardEntry>,
    status: LeaderboardSourceStatus,
    currentSheetId: String,
    currentSheetName: String,
    currentWebhookUrl: String,
    gameState: GameState?,
    calculatedScore: Int,
    isSubmitting: Boolean,
    onRefresh: () -> Unit,
    onSaveSettings: (sheetId: String, sheetName: String, webhook: String) -> Unit,
    onSubmitScore: (playerName: String) -> Unit,
    onGetClipboardRow: (playerName: String) -> String,
    onGetClipboardHeaderAndRow: (playerName: String) -> String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedFactionFilter by remember { mutableStateOf<FactionId?>(null) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showSubmitModal by remember { mutableStateOf(false) }

    val filteredEntries = remember(entries, selectedFactionFilter) {
        if (selectedFactionFilter == null) {
            entries
        } else {
            entries.filter { it.factionId == selectedFactionFilter }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, GoldAccent.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .testTag("leaderboard_dialog_container"),
            color = SkyrimDarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.15f))
                                .border(1.dp, GoldAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "ЧЕМПИОНЫ СКАЙРИМА",
                                color = GoldAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Рейтинг на основе Google Таблицы",
                                color = ParchmentDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp).testTag("close_leaderboard_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = ParchmentLight)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Status Banner / Source Info
                Surface(
                    color = SkyrimSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val (dotColor, statusText) = when (status) {
                                is LeaderboardSourceStatus.OnlineSuccess -> Pair(
                                    HealthGreen,
                                    "Google Таблица онлайн • Синхр: ${status.lastSyncTime}"
                                )
                                is LeaderboardSourceStatus.OfflineCache -> Pair(
                                    Color(0xFFE5A93C),
                                    if (currentSheetId.isBlank()) "Локальный рейтинг (Google Таблица не указана)" else "Локальный кэш • ${status.message}"
                                )
                                is LeaderboardSourceStatus.Loading -> Pair(
                                    GoldAccent,
                                    "Загрузка из Google Таблицы..."
                                )
                                is LeaderboardSourceStatus.Error -> Pair(
                                    AlertFire,
                                    "Ошибка: ${status.message}"
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )

                            Text(
                                text = statusText,
                                color = ParchmentLight,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Rotate animation for refresh icon
                            val infiniteTransition = rememberInfiniteTransition(label = "refresh")
                            val angle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotate"
                            )

                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.size(32.dp).testTag("refresh_leaderboard_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Обновить",
                                    tint = GoldAccent,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .rotate(if (status is LeaderboardSourceStatus.Loading) angle else 0f)
                                )
                            }

                            IconButton(
                                onClick = { showSettingsModal = true },
                                modifier = Modifier.size(32.dp).testTag("open_sheet_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Настройки Google Таблицы",
                                    tint = ParchmentLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // If sheet is not configured yet, show a helpful setup banner
                if (currentSheetId.isBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = SkyrimSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSettingsModal = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Google Таблица не подключена. Нажмите для быстрой настройки в 1 клик",
                                    color = ParchmentLight,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons Row (Open Sheet, Fast Create, Submit Score)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Open Google Sheet or Connect
                    OutlinedButton(
                        onClick = {
                            if (currentSheetId.isNotBlank()) {
                                val url = "https://docs.google.com/spreadsheets/d/$currentSheetId"
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Ссылка: $url", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                showSettingsModal = true
                            }
                        },
                        modifier = Modifier.height(34.dp).testTag("open_sheet_in_browser_button"),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (currentSheetId.isNotBlank()) HealthGreen else GoldAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (currentSheetId.isNotBlank()) Icons.Default.TableChart else Icons.Default.AddLink,
                            contentDescription = null,
                            tint = if (currentSheetId.isNotBlank()) HealthGreen else GoldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentSheetId.isNotBlank()) "Открыть Google Таблицу" else "Подключить Google Таблицу",
                            color = ParchmentLight,
                            fontSize = 11.sp
                        )
                    }

                    // Fast Create New Sheet if not yet configured
                    if (currentSheetId.isBlank()) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sheets.new"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Откройте sheets.new в браузере", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.height(34.dp).testTag("create_new_sheet_button"),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HealthGreen.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Создать таблицу (sheets.new)", color = HealthGreen, fontSize = 11.sp)
                        }
                    }

                    // Submit Current Campaign Score
                    Button(
                        onClick = { showSubmitModal = true },
                        modifier = Modifier.height(34.dp).testTag("open_submit_score_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Записать результат ($calculatedScore)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Faction Filter Chips (Horizontal Scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFactionFilter == null,
                        onClick = { selectedFactionFilter = null },
                        label = { Text("Все фракции (${entries.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent.copy(alpha = 0.25f),
                            selectedLabelColor = GoldAccent
                        ),
                        modifier = Modifier.height(30.dp)
                    )

                    FactionsData.allFactions.forEach { faction ->
                        val count = entries.count { it.factionId == faction.id }
                        FilterChip(
                            selected = selectedFactionFilter == faction.id,
                            onClick = { selectedFactionFilter = faction.id },
                            label = { Text("${faction.nameRu} ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = faction.primaryColor.copy(alpha = 0.3f),
                                selectedLabelColor = faction.primaryColor
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Leaderboard List
                if (filteredEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Leaderboard, contentDescription = null, tint = ParchmentDark, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Нет записей в этой категории", color = ParchmentDark, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredEntries, key = { "${it.rank}_${it.playerName}_${it.date}" }) { entry ->
                            LeaderboardRowItem(entry = entry)
                        }
                    }
                }
            }
        }
    }

    // Modal: Settings for Google Sheets
    if (showSettingsModal) {
        GoogleSheetsSettingsDialog(
            currentSheetId = currentSheetId,
            currentSheetName = currentSheetName,
            currentWebhookUrl = currentWebhookUrl,
            onSave = { sheetId, sheetName, webhook ->
                onSaveSettings(sheetId, sheetName, webhook)
                showSettingsModal = false
                Toast.makeText(context, "Настройки сохранены, обновляем рейтинг...", Toast.LENGTH_SHORT).show()
            },
            onClose = { showSettingsModal = false }
        )
    }

    // Modal: Submit Campaign Score
    if (showSubmitModal) {
        SubmitScoreDialog(
            gameState = gameState,
            calculatedScore = calculatedScore,
            currentSheetId = currentSheetId,
            currentWebhookUrl = currentWebhookUrl,
            isSubmitting = isSubmitting,
            onSubmit = { playerName ->
                // Automatically copy the ready row to clipboard so player can paste in 1 tap
                val row = onGetClipboardRow(playerName)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Skyrim War Google Sheets Row", row)
                clipboard.setPrimaryClip(clip)

                onSubmitScore(playerName)
                showSubmitModal = false
            },
            onCopyCsv = { playerName ->
                val row = onGetClipboardRow(playerName)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Skyrim War Google Sheets Row", row)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Строка рекорда скопирована в буфер обмена!", Toast.LENGTH_SHORT).show()
            },
            onCopyHeaderAndRow = { playerName ->
                val content = onGetClipboardHeaderAndRow(playerName)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Skyrim War Full Table Template", content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Шапка таблицы и ваш результат скопированы в буфер!", Toast.LENGTH_LONG).show()
            },
            onOpenSheet = {
                val url = if (currentSheetId.isNotBlank()) {
                    "https://docs.google.com/spreadsheets/d/$currentSheetId"
                } else {
                    "https://sheets.new"
                }
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Откройте $url в браузере", Toast.LENGTH_SHORT).show()
                }
            },
            onClose = { showSubmitModal = false }
        )
    }
}

@Composable
private fun LeaderboardRowItem(entry: LeaderboardEntry) {
    val faction = FactionsData.getFaction(entry.factionId)

    // Top 3 badges
    val (rankBadgeColor, rankBorderColor) = when (entry.rank) {
        1 -> Pair(GoldAccent.copy(alpha = 0.25f), GoldAccent)
        2 -> Pair(Color(0xFFC0C0C0).copy(alpha = 0.25f), Color(0xFFC0C0C0))
        3 -> Pair(Color(0xFFCD7F32).copy(alpha = 0.25f), Color(0xFFCD7F32))
        else -> Pair(SkyrimSurfaceVariant, SkyrimBorder)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (entry.isPlayer) 1.5.dp else 1.dp,
                color = if (entry.isPlayer) GoldAccent else rankBorderColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp)
            ),
        color = if (entry.isPlayer) GoldAccent.copy(alpha = 0.08f) else SkyrimSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Rank & Hero Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Rank Circle
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(rankBadgeColor)
                        .border(1.dp, rankBorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (entry.rank <= 3) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = rankBorderColor,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "#${entry.rank}",
                            color = ParchmentLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Hero & Faction Details
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.playerName,
                            color = if (entry.isPlayer) GoldAccent else ParchmentLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.isPlayer) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = GoldAccent,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "ВЫ",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Faction Badge
                        Surface(
                            color = faction.primaryColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, faction.primaryColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = faction.nameRu,
                                color = faction.primaryColor,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        // Sub stats
                        Text(
                            text = "🏰 ${entry.holdsCaptured} • ⏳ ${entry.daysSurvived} дн • ${entry.outcome}",
                            color = ParchmentDark,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Right: Score & Date
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.score} очков",
                    color = GoldAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = entry.date,
                    color = ParchmentDark,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun GoogleSheetsSettingsDialog(
    currentSheetId: String,
    currentSheetName: String,
    currentWebhookUrl: String,
    onSave: (sheetId: String, sheetName: String, webhook: String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var sheetInput by remember { mutableStateOf(currentSheetId) }
    var sheetNameInput by remember { mutableStateOf(currentSheetName.ifBlank { "Sheet1" }) }
    var webhookInput by remember { mutableStateOf(currentWebhookUrl) }

    // Real-time extracted ID from pasted URL
    val extractedId = remember(sheetInput) {
        val trimmed = sheetInput.trim()
        if (trimmed.contains("/spreadsheets/d/")) {
            val parts = trimmed.split("/spreadsheets/d/")
            if (parts.size > 1) {
                parts[1].split("/").first().split("?").first().split("#").first()
            } else ""
        } else trimmed
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            color = SkyrimSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(24.dp))
                        Text("Подключение Google Таблицы", color = GoldAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = ParchmentLight)
                    }
                }

                Text(
                    text = "Подключите свою Google Таблицу для сохранения и синхронизации рекордов между игроками.",
                    color = ParchmentDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Quick Action Buttons (Create Sheet in 1 tap, Copy Headers)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sheets.new"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Откройте sheets.new в браузере", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(38.dp).testTag("settings_create_sheet_btn"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HealthGreen),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Создать (sheets.new)", color = ParchmentLight, fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val header = "Имя\tФракция\tОчки\tТвердыни\tДни\tИтог\tДата"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Google Sheets Headers", header)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Шапка колонок скопирована! Вставьте в ячейку A1 в таблице", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f).height(38.dp).testTag("settings_copy_headers_btn"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorderLight),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Скопировать шапку", color = ParchmentLight, fontSize = 10.sp)
                    }
                }

                // Step-by-Step Instructions
                Surface(
                    color = SkyrimSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("3 ПРОСТЫХ ШАГА ДЛЯ ПОДКЛЮЧЕНИЯ:", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("1. Нажмите «Создать (sheets.new)» выше — откроется чистая Google Таблица.", color = ParchmentLight, fontSize = 11.sp)
                        Text("2. Вставьте скопированную шапку в верхнюю ячейку A1 (Ctrl+V или Долгое нажатие -> Вставить).", color = ParchmentLight, fontSize = 11.sp)
                        Text("3. В Google Таблице нажмите кнопку «Поделиться» (справа вверху) -> «Все, у кого есть ссылка» (Читатель), скопируйте ссылку и вставьте её ниже.", color = HealthGreen, fontSize = 11.sp)
                    }
                }

                // Input Sheet ID or URL
                OutlinedTextField(
                    value = sheetInput,
                    onValueChange = { sheetInput = it },
                    label = { Text("Ссылка на Google Таблицу или ID", fontSize = 11.sp) },
                    placeholder = { Text("https://docs.google.com/spreadsheets/d/...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("sheet_id_input"),
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ParchmentLight,
                        unfocusedTextColor = ParchmentLight,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = SkyrimBorder
                    )
                )

                if (extractedId.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Распознан ID: $extractedId",
                            color = HealthGreen,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Sheet Name Input
                OutlinedTextField(
                    value = sheetNameInput,
                    onValueChange = { sheetNameInput = it },
                    label = { Text("Имя листа (по умолчанию: Sheet1 или Лист1)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("sheet_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ParchmentLight,
                        unfocusedTextColor = ParchmentLight,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = SkyrimBorder
                    )
                )

                // Webhook URL (Optional)
                OutlinedTextField(
                    value = webhookInput,
                    onValueChange = { webhookInput = it },
                    label = { Text("URL Google Apps Script (Веб-хук для авто-записи, опционально)", fontSize = 11.sp) },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("webhook_url_input"),
                    singleLine = false,
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ParchmentLight,
                        unfocusedTextColor = ParchmentLight,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = SkyrimBorder
                    )
                )

                // Apps Script 1-tap guide card
                Surface(
                    color = SkyrimDarkBackground,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                            Text("Как включить авто-запись в Google Таблицу:", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "1. В Google Таблице откройте: Расширения -> Apps Script.\n" +
                            "2. Вставьте скрипт (кнопка ниже скопирует готовый код).\n" +
                            "3. Нажмите: Развернуть -> Новое развертывание -> Веб-приложение -> Доступ: «Все» -> Развернуть.\n" +
                            "4. Скопируйте ссылку и вставьте в поле выше.",
                            color = ParchmentLight,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                        OutlinedButton(
                            onClick = {
                                val script = """
                                    function doPost(e) {
                                      var data = JSON.parse(e.postData.contents);
                                      var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
                                      sheet.appendRow([data.playerName, data.faction, data.score, data.daysSurvived]);
                                      return ContentService.createTextOutput("OK");
                                    }
                                """.trimIndent()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Google Apps Script", script)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Код скрипта скопирован в буфер обмена!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Скопировать готовый код скрипта", color = GoldAccent, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { onSave(sheetInput.trim(), sheetNameInput.trim(), webhookInput.trim()) },
                    modifier = Modifier.fillMaxWidth().testTag("save_sheet_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранить и подключить", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SubmitScoreDialog(
    gameState: GameState?,
    calculatedScore: Int,
    currentSheetId: String,
    currentWebhookUrl: String,
    isSubmitting: Boolean,
    onSubmit: (playerName: String) -> Unit,
    onCopyCsv: (playerName: String) -> Unit,
    onCopyHeaderAndRow: (playerName: String) -> Unit,
    onOpenSheet: () -> Unit,
    onClose: () -> Unit
) {
    var playerName by remember { mutableStateOf("Довакин") }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, GoldAccent, RoundedCornerShape(14.dp)),
            color = SkyrimSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
                        Text("Записать результат кампании", color = GoldAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = ParchmentLight)
                    }
                }

                // Hero Name Input
                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    label = { Text("Имя героя / полководца", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("submit_player_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ParchmentLight,
                        unfocusedTextColor = ParchmentLight,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = SkyrimBorder
                    )
                )

                // Score Calculation Breakdown
                Surface(
                    color = SkyrimSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("РАСЧЕТ БОЕВЫХ ОЧКОВ:", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (gameState != null) {
                            val faction = FactionsData.getFaction(gameState.playerFactionId)
                            val holds = gameState.getFactionControlledLocationsCount(faction.id)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Фракция:", color = ParchmentDark, fontSize = 11.sp)
                                Text(faction.nameRu, color = faction.primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Удержание твердынь ($holds):", color = ParchmentDark, fontSize = 11.sp)
                                Text("+${holds * 1200}", color = HealthGreen, fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Выиграно битв (${gameState.battlesWon}):", color = ParchmentDark, fontSize = 11.sp)
                                Text("+${gameState.battlesWon * 350}", color = HealthGreen, fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Казна (${gameState.gold}):", color = ParchmentDark, fontSize = 11.sp)
                                Text("+${gameState.gold / 2}", color = GoldAccent, fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Дней войны (${gameState.gameDay}):", color = ParchmentDark, fontSize = 11.sp)
                                Text("+${gameState.gameDay * 45}", color = ParchmentLight, fontSize = 11.sp)
                            }
                            Divider(color = SkyrimBorder, thickness = 0.5.dp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ИТОГОВЫЙ СЧЕТ:", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("$calculatedScore очков", color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // Guidance card explaining how Google Sheets recording works
                if (currentWebhookUrl.isBlank()) {
                    Surface(
                        color = SkyrimDarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                Text("Как результат попадет в Google Таблицу:", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "1. Нажатие кнопки ниже сохранит рекорд в Зал Славы игры и АВТОМАТИЧЕСКИ скопирует готовую строку в буфер обмена.\n" +
                                "2. Откройте Google Таблицу по зеленой кнопке ниже и просто вставьте строку (Вставить / Paste).\n\n" +
                                "💡 Для моментальной авто-отправки прямо по кнопке настройте Веб-хук в ⚙️ Настройках.",
                                color = ParchmentLight,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Primary Save button
                val hasWebhook = currentWebhookUrl.isNotBlank()
                Button(
                    onClick = { onSubmit(playerName.trim()) },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().testTag("confirm_submit_score_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasWebhook) "Отправка в Google Таблицу..." else "Сохранение...", color = Color.Black, fontSize = 12.sp)
                    } else {
                        Icon(if (hasWebhook) Icons.Default.CloudUpload else Icons.Default.Save, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasWebhook) "Отправить в Google Таблицу" else "Записать в рейтинг (+ скопировать)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Copy row only (for appending to existing table)
                OutlinedButton(
                    onClick = { onCopyCsv(playerName.trim()) },
                    modifier = Modifier.fillMaxWidth().testTag("copy_csv_row_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorderLight)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ParchmentLight, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Скопировать строку рекорда", color = ParchmentLight, fontSize = 11.sp)
                }

                // Copy full template (headers + row for new table)
                OutlinedButton(
                    onClick = { onCopyHeaderAndRow(playerName.trim()) },
                    modifier = Modifier.fillMaxWidth().testTag("copy_full_template_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Скопировать шапку + результат", color = GoldAccent, fontSize = 11.sp)
                }

                // Open / Create Google Sheet button
                OutlinedButton(
                    onClick = onOpenSheet,
                    modifier = Modifier.fillMaxWidth().testTag("open_or_create_sheet_btn"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HealthGreen.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (currentSheetId.isNotBlank()) "Открыть Google Таблицу" else "Создать Google Таблицу (sheets.new)",
                        color = HealthGreen,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
