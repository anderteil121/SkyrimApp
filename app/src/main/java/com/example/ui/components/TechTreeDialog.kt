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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GameState
import com.example.data.model.TechTier
import com.example.ui.theme.*

@Composable
fun TechTreeDialog(
    gameState: GameState,
    onUpgradeTech: () -> Unit,
    onClose: () -> Unit
) {
    val currentTier = gameState.techTier
    val nextTier = when (currentTier) {
        TechTier.IRON -> TechTier.STEEL
        TechTier.STEEL -> TechTier.ELVEN_ORCISH
        TechTier.ELVEN_ORCISH -> TechTier.EBONY_GLASS
        TechTier.EBONY_GLASS -> TechTier.DRAGONBONE
        TechTier.DRAGONBONE -> null
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .testTag("tech_tree_dialog"),
            color = SkyrimSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Кузница Скайрима: Экипировка", color = ParchmentLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = ParchmentDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TechTier.values().forEach { tier ->
                    val isCurrent = tier == currentTier
                    val isUnlocked = tier.ordinal <= currentTier.ordinal

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = if (isCurrent) SkyrimSurfaceVariant else SkyrimDarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCurrent) GoldAccent else if (isUnlocked) HealthGreen.copy(alpha = 0.5f) else SkyrimBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isUnlocked) GoldAccent else SkyrimBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${tier.ordinal + 1}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tier.titleRu,
                                    color = if (isUnlocked) ParchmentLight else ParchmentDark,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Атака: +${((tier.attackBonus - 1f) * 100).toInt()}% • Защита: +${((tier.defenseBonus - 1f) * 100).toInt()}%",
                                    color = if (isUnlocked) HealthGreen else ParchmentDark,
                                    fontSize = 11.sp
                                )
                            }
                            if (isCurrent) {
                                Badge(containerColor = GoldAccent) {
                                    Text("Текущее", color = Color.Black, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (nextTier != null) {
                    val canAfford = gameState.gold >= nextTier.upgradeCostGold && gameState.ore >= nextTier.upgradeCostOre
                    Button(
                        onClick = onUpgradeTech,
                        enabled = canAfford,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upgrade_tech_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text(
                            text = "Выковать ${nextTier.titleRu.split(" ").first()} (${nextTier.upgradeCostGold}G, ${nextTier.upgradeCostOre} руды)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text("Достигнуто максимальное вооружение (Драконье)!", color = GoldAccent, fontSize = 12.sp)
                }
            }
        }
    }
}
