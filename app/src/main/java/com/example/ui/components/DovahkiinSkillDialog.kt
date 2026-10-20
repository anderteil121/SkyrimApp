package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun DovahkiinSkillDialog(
    gameState: GameState,
    onMoveDovahkiin: (String) -> Unit,
    onClose: () -> Unit
) {
    val currentLoc = gameState.locations.firstOrNull { it.id == gameState.dovahkiin.currentLocationId }
    val friendlyLocations = gameState.locations.filter { it.ownerFactionId == gameState.playerFactionId }

    var isRelocating by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .testTag("dovahkiin_dialog"),
            color = SkyrimSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, DovahkiinCyan)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DovahkiinCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DovahkiinCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Довакин (Драконорожденный)", color = ParchmentLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Дислокация: ${currentLoc?.nameRu ?: "Неизвестно"}", color = DovahkiinCyan, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = ParchmentDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Relocate Dovahkiin Button / Selector
                if (!isRelocating) {
                    OutlinedButton(
                        onClick = { isRelocating = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DovahkiinCyan)
                    ) {
                        Icon(Icons.Default.NearMe, contentDescription = null, tint = DovahkiinCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Переместить Довакина в другую цитадель", color = DovahkiinCyan, fontSize = 12.sp)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SkyrimSurfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text("Выберите союзную твердыню:", color = ParchmentLight, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            friendlyLocations.forEach { loc ->
                                FilterChip(
                                    selected = loc.id == currentLoc?.id,
                                    onClick = {
                                        onMoveDovahkiin(loc.id)
                                        isRelocating = false
                                    },
                                    label = { Text(loc.nameRu, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DovahkiinCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Слова Силы и Крики (Ту'ум):", color = ParchmentDark, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                // List of 4 shouts
                ShoutId.entries.forEach { shout ->
                    val remainingCooldown = gameState.shoutCooldowns[shout] ?: 0
                    val isReady = remainingCooldown == 0
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = SkyrimDarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isReady) DovahkiinCyan.copy(alpha = 0.5f) else SkyrimBorder
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = shout.words,
                                    color = if (isReady) DovahkiinCyan else ParchmentDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (isReady) "Готов (${shout.cooldownSeconds}с)" else "Откат: ${remainingCooldown}с",
                                    color = if (isReady) HealthGreen else AlertFire,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = shout.nameRu,
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = shout.descriptionRu,
                                color = ParchmentBase,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
