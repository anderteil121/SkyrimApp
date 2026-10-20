package com.example.ui.components

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
import androidx.compose.runtime.*
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
fun WarFeedDrawer(
    feedMessages: List<FeedMessage>,
    playerFactionId: FactionId,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .testTag("war_feed_drawer"),
        color = SkyrimSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorder),
        tonalElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Сводки фронта Скайрима",
                        color = ParchmentLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = ParchmentDark)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips with horizontal scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "Все",
                    "SIEGE" to "Штурмы",
                    "CONQUEST" to "Захваты",
                    "MINE" to "Моя фракция"
                ).forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val filteredList = feedMessages.filter { msg ->
                when (selectedFilter) {
                    "SIEGE" -> msg.type == FeedType.WAR_ALERT
                    "CONQUEST" -> msg.type == FeedType.CONQUEST
                    "MINE" -> msg.factionId == playerFactionId
                    else -> true
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет сообщений по выбранному фильтру", color = ParchmentDark, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { msg ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SkyrimSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (msg.type) {
                                    FeedType.WAR_ALERT -> AlertFire.copy(alpha = 0.6f)
                                    FeedType.CONQUEST -> GoldAccent.copy(alpha = 0.6f)
                                    else -> SkyrimBorder
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (msg.type) {
                                                        FeedType.WAR_ALERT -> AlertFire
                                                        FeedType.CONQUEST -> GoldAccent
                                                        FeedType.REINFORCEMENTS -> HealthGreen
                                                        else -> ParchmentDark
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when (msg.type) {
                                                FeedType.WAR_ALERT -> "БОЕВАЯ ТРЕВОГА"
                                                FeedType.CONQUEST -> "ТЕРРИТОРИАЛЬНЫЙ ЗАХВАТ"
                                                FeedType.REINFORCEMENTS -> "МАРШ ВОЙСК"
                                                FeedType.AMBUSH -> "ОБОРОНА РУБЕЖЕЙ"
                                                FeedType.DIPLOMACY -> "СИТУАЦИЯ В СКАЙРИМЕ"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (msg.type) {
                                                FeedType.WAR_ALERT -> AlertFire
                                                FeedType.CONQUEST -> GoldAccent
                                                else -> ParchmentDark
                                            }
                                        )
                                    }

                                    Text(
                                        text = msg.formattedTime,
                                        fontSize = 10.sp,
                                        color = ParchmentDark
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = msg.textRu,
                                    fontSize = 12.sp,
                                    color = ParchmentLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
