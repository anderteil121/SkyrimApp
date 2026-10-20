package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GameResult
import com.example.data.model.GameState
import com.example.ui.theme.*

@Composable
fun GameOverDialog(
    gameState: GameState,
    onContinueSandbox: () -> Unit,
    onRestartNewGame: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val isVictory = gameState.gameResult == GameResult.VICTORY
    val faction = gameState.playerFaction

    Dialog(
        onDismissRequest = onContinueSandbox,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .testTag("game_over_dialog"),
            color = SkyrimSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (isVictory) GoldAccent else AlertFire
            ),
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trophy / Skull Emblem
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (isVictory) GoldAccent.copy(alpha = 0.2f) else AlertFire.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVictory) Icons.Default.EmojiEvents else Icons.Default.Dangerous,
                        contentDescription = null,
                        tint = if (isVictory) GoldAccent else AlertFire,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val controlledCount = gameState.getFactionControlledLocationsCount(faction.id)
                val isEarlyEnd = !isVictory && controlledCount > 0

                // Title
                Text(
                    text = when {
                        isVictory -> "ТРИУМФ СКАЙРИМА!"
                        isEarlyEnd -> "КАМПАНИЯ ЗАВЕРШЕНА"
                        else -> "ПАДЕНИЕ ФРАКЦИИ"
                    },
                    color = if (isVictory || isEarlyEnd) GoldAccent else AlertFire,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = when {
                        isVictory -> "Все форты, города и шахты перешли под знамена фракции '${faction.nameRu}'. Гражданская война окончена полной победой!"
                        isEarlyEnd -> "Военная кампания фракции '${faction.nameRu}' завершена досрочно. Подведены окончательные итоги войны и боевые заслуги."
                        else -> "Ваша фракция '${faction.nameRu}' потеряла последний оплот в провинции. Знамена повержены."
                    },
                    color = ParchmentLight,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campaign Stats Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SkyrimSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ИТОГИ ВОЕННОЙ КАМПАНИИ:",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Длительность войны:", color = ParchmentDark, fontSize = 12.sp)
                            Text("${gameState.gameDay} дней", color = ParchmentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Под контролем крепостей:", color = ParchmentDark, fontSize = 12.sp)
                            Text("${gameState.getFactionControlledLocationsCount(faction.id)} / ${gameState.locations.size}", color = HealthGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Выигранных сражений:", color = ParchmentDark, fontSize = 12.sp)
                            Text("${gameState.battlesWon}", color = ParchmentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Захвачено фортов:", color = ParchmentDark, fontSize = 12.sp)
                            Text("${gameState.fortsCaptured}", color = ParchmentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Казна победы:", color = ParchmentDark, fontSize = 12.sp)
                            Text("${gameState.gold} септимов", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Вооружение:", color = ParchmentDark, fontSize = 12.sp)
                            Text(gameState.techTier.titleRu.split(" ").first(), color = ParchmentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                if (isVictory) {
                    Button(
                        onClick = onContinueSandbox,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("continue_sandbox_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Продолжить свободную игру (Песочница)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Google Sheets Leaderboard button
                Button(
                    onClick = onOpenLeaderboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_leaderboard_from_gameover"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Рейтинг чемпионов (Google Таблица)", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onRestartNewGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_new_campaign_after_gameover"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorderLight)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = ParchmentLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVictory) "Начать новую кампанию за другую фракцию" else "Попробовать снова",
                        color = ParchmentLight,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
