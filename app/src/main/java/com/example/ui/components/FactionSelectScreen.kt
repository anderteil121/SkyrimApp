package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Faction
import com.example.data.model.FactionId
import com.example.data.model.FactionsData
import com.example.ui.theme.*

@Composable
fun FactionSelectScreen(
    hasSavedGame: Boolean,
    onResumeGame: () -> Unit,
    onStartNewGame: (FactionId) -> Unit,
    onOpenLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFactionId by remember { mutableStateOf(FactionId.IMPERIAL) }
    val currentFaction = FactionsData.getFaction(selectedFactionId)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("faction_select_screen"),
        color = SkyrimDarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Title Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "СКАЙРИМ: ВОЙНА ГРУППИРОВОК",
                    color = GoldAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Тактическое противостояние за власть над провинцией",
                    color = ParchmentDark,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resume game button if save exists
            if (hasSavedGame) {
                Button(
                    onClick = onResumeGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_campaign_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Продолжить сохраненную кампанию", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Leaderboard Button
            OutlinedButton(
                onClick = onOpenLeaderboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_leaderboard_from_main_menu"),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.8f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = GoldAccent.copy(alpha = 0.08f)
                )
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Таблица лидеров (Google Таблица)", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Выберите вашу фракцию:", color = ParchmentLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            // Factions List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FactionsData.allFactions) { faction ->
                    val isSelected = faction.id == selectedFactionId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFactionId = faction.id }
                            .testTag("faction_item_${faction.id.name.lowercase()}"),
                        color = if (isSelected) SkyrimSurfaceVariant else SkyrimSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isSelected) faction.primaryColor else SkyrimBorder
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(faction.primaryColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (faction.id) {
                                            FactionId.IMPERIAL -> Icons.Default.Shield
                                            FactionId.STORMCLOAK -> Icons.Default.Flag
                                            FactionId.THALMOR -> Icons.Default.AutoAwesome
                                            FactionId.DAWNGUARD -> Icons.Default.WbSunny
                                            FactionId.VOLKIHAR -> Icons.Default.NightsStay
                                            FactionId.FORSWORN -> Icons.Default.Forest
                                            FactionId.BANDITS -> Icons.Default.Dangerous
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = faction.nameRu,
                                        color = if (isSelected) faction.primaryColor else ParchmentLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Лидер: ${faction.leaderRu} • Столица: ${faction.capitalRu}",
                                        color = ParchmentDark,
                                        fontSize = 11.sp
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedFactionId = faction.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = faction.primaryColor)
                                )
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\"${faction.mottoRu}\"",
                                    color = GoldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = faction.descriptionRu,
                                    color = ParchmentBase,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = SkyrimDarkBackground,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Особый бонус: ${faction.specialBonusRu}",
                                        color = HealthGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start Campaign Button
            Button(
                onClick = { onStartNewGame(selectedFactionId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_campaign_button"),
                colors = ButtonDefaults.buttonColors(containerColor = currentFaction.primaryColor)
            ) {
                Icon(Icons.Default.SportsMartialArts, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ВСТУПИТЬ В ВОЙНУ ЗА ${currentFaction.nameRu.uppercase()}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
