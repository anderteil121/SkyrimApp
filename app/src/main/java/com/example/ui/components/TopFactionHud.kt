package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun TopFactionHud(
    gameState: GameState,
    onTogglePause: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onOpenWarFeed: () -> Unit,
    onOpenTechTree: () -> Unit,
    onOpenShouts: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenExitMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val faction = gameState.playerFaction
    val totalLocations = gameState.locations.size
    val controlledCount = gameState.getFactionControlledLocationsCount(faction.id)
    val isTriumphant = gameState.isGameOver && gameState.gameResult == GameResult.VICTORY

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_faction_hud"),
        color = SkyrimSurface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Row 1: Faction Leader, In-Game Time & Simulation speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Faction Emblem & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(faction.primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = faction.nameRu,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = faction.nameRu,
                                color = ParchmentLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (isTriumphant) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "👑 ПОБЕДА",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Text(
                            text = "${faction.leaderRu} • $controlledCount/$totalLocations узлов",
                            color = ParchmentDark,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                // Time & Speed Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "День ${gameState.gameDay}, %02d:00".format(gameState.gameHour),
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Pause / Play
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("sim_pause_button")
                    ) {
                        Icon(
                            if (gameState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Пауза",
                            tint = if (gameState.isPaused) AlertFire else ParchmentLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Speed 1x / 2x / 3x
                    val nextSpeed = when (gameState.gameSpeed) {
                        1.0f -> 2.0f
                        2.0f -> 3.0f
                        else -> 1.0f
                    }
                    Button(
                        onClick = { onSetSpeed(nextSpeed) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("sim_speed_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyrimSurfaceVariant)
                    ) {
                        Text(
                            text = "${gameState.gameSpeed.toInt()}x",
                            fontSize = 11.sp,
                            color = ParchmentLight
                        )
                    }

                    // Exit / End Game Menu button
                    IconButton(
                        onClick = onOpenExitMenu,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("hud_exit_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Завершить игру и выйти в меню",
                            tint = AlertFire,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: S.T.A.L.K.E.R. Faction War Power Bar (Relative Hold over Skyrim)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SkyrimBorder)
            ) {
                for (f in FactionsData.allFactions) {
                    val count = gameState.getFactionControlledLocationsCount(f.id)
                    val targetWeight = if (count > 0) (count.toFloat() / totalLocations).coerceIn(0.001f, 1f) else 0.001f
                    val animatedWeight = androidx.compose.animation.core.animateFloatAsState(
                        targetValue = targetWeight,
                        animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "power_weight_${f.id.name}"
                    ).value
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(animatedWeight)
                                .background(f.primaryColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Resources & Passive Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Gold
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = "Золото", tint = GoldAccent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "${gameState.gold}", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                // Ore
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Handyman, contentDescription = "Руда", tint = ParchmentBase, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "${gameState.ore}", color = ParchmentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                // Recruits
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = "Рекруты", tint = HealthGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "${gameState.recruits}", color = HealthGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 4: Dedicated Full-Width Quick Capabilities Action Bar (Always visible on mobile)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tech Tier / Forge capability
                AssistChip(
                    onClick = onOpenTechTree,
                    label = {
                        Text(
                            text = "Кузница: ${gameState.techTier.titleRu.split(" ").first()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SkyrimSurfaceVariant,
                        labelColor = ParchmentLight
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("open_tech_tree_button")
                )

                // Dovahkiin Shouts capability
                val readyShoutsCount = ShoutId.entries.count { (gameState.shoutCooldowns[it] ?: 0) == 0 }
                AssistChip(
                    onClick = onOpenShouts,
                    label = {
                        Text(
                            text = "Крики Ту'ум ($readyShoutsCount/4)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DovahkiinCyan, modifier = Modifier.size(14.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SkyrimSurfaceVariant,
                        labelColor = DovahkiinCyan
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("open_shouts_button")
                )

                // S.T.A.L.K.E.R. PDA War Feed capability
                AssistChip(
                    onClick = onOpenWarFeed,
                    label = {
                        val activeBattlesCount = gameState.activeBattles.size
                        Text(
                            text = if (activeBattlesCount > 0) "Сводки: Битв ($activeBattlesCount)" else "Сводки фронта",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        val hasBattles = gameState.activeBattles.isNotEmpty()
                        Icon(
                            imageVector = if (hasBattles) Icons.Default.LocalFireDepartment else Icons.Default.Radio,
                            contentDescription = null,
                            tint = if (hasBattles) AlertFire else ParchmentLight,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (gameState.activeBattles.isNotEmpty()) AlertFire.copy(alpha = 0.2f) else SkyrimSurfaceVariant,
                        labelColor = if (gameState.activeBattles.isNotEmpty()) AlertFire else ParchmentLight
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("open_war_feed_button")
                )

                // Google Sheets Leaderboard / Rating capability
                AssistChip(
                    onClick = onOpenLeaderboard,
                    label = {
                        Text(
                            text = "Рейтинг (Таблица)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = GoldAccent.copy(alpha = 0.15f),
                        labelColor = GoldAccent
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("open_leaderboard_hud_button")
                )

                // Early Exit / Main Menu capability
                AssistChip(
                    onClick = onOpenExitMenu,
                    label = {
                        Text(
                            text = "В меню / Завершить",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = AlertFire,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = AlertFire.copy(alpha = 0.15f),
                        labelColor = ParchmentLight
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("open_exit_menu_hud_chip")
                )
            }
        }
    }
}
