package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import com.example.data.model.GameState
import com.example.ui.theme.*

@Composable
fun EarlyExitDialog(
    gameState: GameState,
    onDismiss: () -> Unit,
    onFinishEarly: () -> Unit,
    onSaveAndExit: () -> Unit,
    onAbandonAndExit: () -> Unit
) {
    val faction = gameState.playerFaction
    val controlledCount = gameState.getFactionControlledLocationsCount(faction.id)
    val totalLocations = gameState.locations.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("early_exit_dialog"),
            color = SkyrimSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.8f)),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(GoldAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "МЕНЮ КАМПАНИИ",
                    color = GoldAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "${faction.nameRu} • День ${gameState.gameDay} • Контроль: $controlledCount/$totalLocations",
                    color = ParchmentDark,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: End Game Early & View/Save Results (Досрочное завершение с подведением итогов)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SkyrimSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AlertFire.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsScore, contentDescription = null, tint = AlertFire, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Завершить войну досрочно",
                                color = ParchmentLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Подвести окончательные итоги войны с текущими достижениями и перейти к экрану результатов для записи рекорда в рейтинг.",
                            color = ParchmentDark,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onFinishEarly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("finish_campaign_early_confirm_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertFire)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Завершить и подвести итоги", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Save Progress and Return to Main Menu
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SkyrimSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SkyrimBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Сохранить и выйти в меню",
                                color = ParchmentLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Сохраняет текущее состояние кампании. Вы сможете продолжить игру в любой момент из главного меню.",
                            color = ParchmentDark,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onSaveAndExit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_and_exit_confirm_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SkyrimSurface)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = ParchmentLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сохранить и выйти в меню", color = ParchmentLight, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 3: Discard and Exit (Без сохранения)
                OutlinedButton(
                    onClick = onAbandonAndExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("abandon_campaign_confirm_button"),
                    border = BorderStroke(1.dp, AlertFire.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AlertFire, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Выйти в меню без сохранения", color = AlertFire, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cancel / Continue
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cancel_early_exit_button")
                ) {
                    Text("Вернуться на карту (Продолжить игру)", color = ParchmentDark, fontSize = 12.sp)
                }
            }
        }
    }
}
