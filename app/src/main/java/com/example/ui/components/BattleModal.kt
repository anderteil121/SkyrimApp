package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun BattleModal(
    battle: Battle,
    location: LocationNode,
    gameState: GameState,
    onSelectTactic: (BattleTactic) -> Unit,
    onCastShout: (ShoutId) -> Unit,
    onClose: () -> Unit
) {
    val attFaction = FactionsData.getFaction(battle.attackerFactionId)
    val defFaction = FactionsData.getFaction(battle.defenderFactionId)

    val currentTactic = gameState.battleTacticsMap[battle.locationId] ?: battle.selectedTactic

    // Smoothly animated progress bars for combat damage
    val animatedAttPercent by animateFloatAsState(
        targetValue = (battle.attackerStrength / battle.maxAttackerStrength.coerceAtLeast(1f)).coerceIn(0f, 1f),
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "att_hp_anim"
    )
    val animatedDefPercent by animateFloatAsState(
        targetValue = (battle.defenderStrength / battle.maxDefenderStrength.coerceAtLeast(1f)).coerceIn(0f, 1f),
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "def_hp_anim"
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .testTag("battle_modal"),
            color = SkyrimSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, AlertFire)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = AlertFire, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Штурм: ${location.nameRu}",
                                color = ParchmentLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Укрепления: ${location.fortificationLevel} ур. • ${location.holdRu}",
                                color = ParchmentDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Свернуть", tint = ParchmentDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Faction vs Faction Combat Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Attackers
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = SkyrimSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, attFaction.primaryColor)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(attFaction.primaryColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "АТАКА: ${attFaction.nameRu}",
                                    color = attFaction.primaryColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${battle.attackerStrength.toInt()} HP",
                                    color = ParchmentLight,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${battle.totalAttackerSoldiers} бойцов",
                                    color = GoldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { animatedAttPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = attFaction.primaryColor,
                                trackColor = SkyrimBorder
                            )
                            if (battle.attackerSquads.size > 1) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "⚔️ Объединено ${battle.attackerSquads.size} отряда(ов)",
                                    color = DovahkiinCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Defenders
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = SkyrimSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, defFaction.primaryColor)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(defFaction.primaryColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ОБОРОНА: ${defFaction.nameRu}",
                                    color = defFaction.primaryColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${battle.defenderStrength.toInt()} HP",
                                    color = ParchmentLight,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${battle.totalDefenderSoldiers} бойцов",
                                    color = ParchmentLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { animatedDefPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = defFaction.primaryColor,
                                trackColor = SkyrimBorder
                            )
                        }
                    }
                }

                // Breakdown of participating squads (shows how forces sum up)
                if (battle.attackerSquads.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SkyrimSurfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = "СОСТАВ ОСАДНОЙ АРМИИ (СУММА СИЛ):",
                                color = GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (sq in battle.attackerSquads) {
                                    Surface(
                                        color = SkyrimDarkBackground,
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SkyrimBorderLight)
                                    ) {
                                        Text(
                                            text = "${sq.nameRu} (${sq.count})",
                                            color = ParchmentLight,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dovahkiin Shouts Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SkyrimSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DovahkiinCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DovahkiinCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Драконьи Крики Ту'ума (Довакин):", color = DovahkiinCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (shout in ShoutId.entries) {
                                val cooldown = gameState.shoutCooldowns[shout] ?: 0
                                val isReady = cooldown == 0

                                Button(
                                    onClick = { onCastShout(shout) },
                                    enabled = isReady,
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("cast_shout_${shout.name.lowercase()}"),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DovahkiinCyan.copy(alpha = 0.85f),
                                        disabledContainerColor = SkyrimBorder
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isReady) Color.Black else ParchmentDark,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isReady) shout.words else "${shout.words.split(" ").first()} (${cooldown}с)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isReady) Color.Black else ParchmentDark
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Battle Tactics Row (Horizontally scrollable with complete readable titles)
                Text("Тактический приказ войскам:", color = ParchmentDark, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        BattleTactic.BALANCED,
                        BattleTactic.SHIELD_WALL,
                        BattleTactic.ARCHER_VOLLEY,
                        BattleTactic.FLANK_ATTACK,
                        BattleTactic.ALL_OUT_CHARGE,
                        BattleTactic.RETREAT
                    ).forEach { tactic ->
                        val isSelected = tactic == currentTactic
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectTactic(tactic) },
                            label = {
                                Text(
                                    text = tactic.titleRu,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live Combat Log
                Text("Хроника сражения:", color = ParchmentDark, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = SkyrimDarkBackground,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorder)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        reverseLayout = true
                    ) {
                        items(battle.log.reversed()) { logEntry ->
                            Text(
                                text = "• $logEntry",
                                color = when {
                                    logEntry.contains("ПОДКРЕПЛЕНИЕ") -> GoldAccent
                                    logEntry.contains("ДОВАКИН") -> DovahkiinCyan
                                    else -> ParchmentLight
                                },
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
