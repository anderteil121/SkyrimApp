package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
fun LocationActionSheet(
    gameState: GameState,
    location: LocationNode,
    onRecruitGarrison: (String, Int) -> Unit,
    onUpgradeFortification: (String) -> Unit,
    onDispatchSquad: (fromId: String, targetId: String, unitClass: UnitClass, count: Int, isDovahkiin: Boolean) -> Unit,
    onDispatchMassAssault: (fromIds: List<String>, targetId: String, unitClass: UnitClass, count: Int) -> Unit = { _, _, _, _ -> },
    onOpenBattle: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: androidx.compose.ui.unit.Dp? = null
) {
    val ownerFaction = FactionsData.getFaction(location.ownerFactionId)
    val isFriendly = location.ownerFactionId == gameState.playerFactionId
    val isDovahkiinHere = gameState.dovahkiin.currentLocationId == location.id

    // Friendly base dispatch state
    var isDispatching by remember(location.id) { mutableStateOf(false) }
    var selectedTargetId by remember(location.id) {
        mutableStateOf(location.connections.firstOrNull() ?: "")
    }
    var selectedUnitClass by remember { mutableStateOf(UnitClass.INFANTRY) }
    var selectedSquadCount by remember { mutableStateOf(20) }
    var sendWithDovahkiin by remember { mutableStateOf(false) }

    // Hostile base assault options: find connected friendly bases
    val connectedFriendlyBases = remember(location.id, gameState.locations) {
        location.connections.mapNotNull { connId ->
            gameState.locations.firstOrNull { it.id == connId && it.ownerFactionId == gameState.playerFactionId }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("location_action_sheet"),
        color = SkyrimSurface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorder),
        tonalElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (maxHeight != null) Modifier.heightIn(max = maxHeight) else Modifier.heightIn(max = 520.dp))
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Mobile Drag Handle Indicator
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(SkyrimBorderLight)
                    .align(Alignment.CenterHorizontally)
            )

            // Header Row: Type badge, Name, Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ownerFaction.primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (location.type) {
                                LocationType.CITY -> Icons.Default.LocationCity
                                LocationType.FORT, LocationType.STRONGHOLD -> Icons.Default.Castle
                                LocationType.MINE -> Icons.Default.Handyman
                                LocationType.SETTLEMENT -> Icons.Default.Home
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = location.nameRu,
                            color = ParchmentLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${location.type.titleRu} • Владение ${location.holdRu}",
                            color = ParchmentDark,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = ParchmentDark)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Owner banner & Dovahkiin stationed badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Под контролем: ",
                        color = ParchmentDark,
                        fontSize = 12.sp
                    )
                    Text(
                        text = ownerFaction.nameRu,
                        color = ownerFaction.primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (isDovahkiinHere) {
                    Badge(containerColor = DovahkiinCyan.copy(alpha = 0.2f)) {
                        Text("Довакин здесь", color = DovahkiinCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Garrison & Fortification level
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SkyrimSurfaceVariant)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Гарнизон", color = ParchmentDark, fontSize = 11.sp)
                    Text(
                        text = "${location.garrison} / ${location.maxGarrison} воинов",
                        color = if (location.garrison < 20) AlertFire else ParchmentLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Укрепления (ур. ${location.fortificationLevel})", color = ParchmentDark, fontSize = 11.sp)
                    Row {
                        repeat(5) { index ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < location.fortificationLevel) GoldAccent else SkyrimBorder,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Production stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = SkyrimSurfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+${location.goldIncome}/цикл", color = ParchmentLight, fontSize = 11.sp)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = SkyrimSurfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Handyman, contentDescription = null, tint = ParchmentBase, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+${location.oreIncome} руды", color = ParchmentLight, fontSize = 11.sp)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = SkyrimSurfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = HealthGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+${location.recruitIncome} рекр.", color = ParchmentLight, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SIEGE ACTIVE!
            if (location.isUnderSiege) {
                val battleAtLoc = gameState.activeBattles.firstOrNull { it.locationId == location.id }
                val isPlayerInvolved = battleAtLoc != null && battleAtLoc.isPlayerInvolved(gameState.playerFactionId)
                if (isPlayerInvolved) {
                    Button(
                        onClick = { onOpenBattle(location.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_battle_modal_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertFire)
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ВСТУПИТЬ В БИТВУ (ШТУРМ В РАЗГАРЕ!)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Battle between two other factions: player cannot watch or intervene!
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("foreign_battle_notice"),
                        color = SkyrimSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertFire.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = AlertFire, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                val attFaction = battleAtLoc?.let { FactionsData.getFaction(it.attackerFactionId).nameRu } ?: "Враги"
                                val defFaction = battleAtLoc?.let { FactionsData.getFaction(it.defenderFactionId).nameRu } ?: "Оборона"
                                Text(
                                    text = "ИДЁТ ШТУРМ: $attFaction vs $defFaction",
                                    color = AlertFire,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Сражение сторонних сил. Ваша фракция не участвует в этой битве.",
                                    color = ParchmentDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ACTIONS
            if (isFriendly) {
                if (!isDispatching) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Recruit Garrison (strictly respects max garrison limits and avoids taking money if full)
                        val availableGarrisonSlots = (location.maxGarrison - location.garrison).coerceAtLeast(0)
                        val recruitCount = 15.coerceAtMost(availableGarrisonSlots)
                        val recruitGoldCost = recruitCount * 6
                        val canRecruitGarrison = availableGarrisonSlots > 0 && gameState.gold >= recruitGoldCost && gameState.recruits >= recruitCount

                        OutlinedButton(
                            onClick = { onRecruitGarrison(location.id, recruitCount) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("recruit_garrison_button"),
                            enabled = canRecruitGarrison,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (canRecruitGarrison) SkyrimBorderLight else SkyrimBorder
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (availableGarrisonSlots > 0) "Нанять гарнизон" else "Гарнизон полон",
                                    fontSize = 11.sp,
                                    color = if (canRecruitGarrison) ParchmentLight else ParchmentDark
                                )
                                Text(
                                    text = if (availableGarrisonSlots > 0) "+$recruitCount бойцов (${recruitGoldCost}G)" else "Максимум (${location.maxGarrison})",
                                    fontSize = 10.sp,
                                    color = if (availableGarrisonSlots > 0) GoldAccent else AlertFire
                                )
                            }
                        }

                        // Upgrade Fortifications
                        OutlinedButton(
                            onClick = { onUpgradeFortification(location.id) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upgrade_fort_button"),
                            enabled = location.fortificationLevel < 5 && gameState.gold >= (location.fortificationLevel * 160),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorderLight)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Укрепить стены", fontSize = 11.sp)
                                Text("${location.fortificationLevel * 160}G / ${location.fortificationLevel * 90} руды", fontSize = 10.sp, color = ParchmentDark)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { isDispatching = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_dispatch_panel_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyrimSurfaceVariant)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сформировать и отправить отряд по тракту", color = ParchmentLight)
                    }
                } else {
                    // Squad Dispatch Sub-panel from friendly base
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SkyrimSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Куда направить войско:",
                            color = ParchmentLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Target selection dropdown / chips with horizontal scroll
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (connId in location.connections) {
                                val targetLoc = gameState.locations.firstOrNull { it.id == connId }
                                if (targetLoc != null) {
                                    val isTargetSelected = targetLoc.id == selectedTargetId
                                    FilterChip(
                                        selected = isTargetSelected,
                                        onClick = { selectedTargetId = targetLoc.id },
                                        label = { Text(targetLoc.nameRu, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldAccent,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Unit Class Selection with horizontal scroll
                        Text("Род войск:", color = ParchmentDark, fontSize = 11.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(UnitClass.RECON, UnitClass.INFANTRY, UnitClass.ARCHERS, UnitClass.MAGES, UnitClass.ELITE).forEach { uClass ->
                                val isSelected = uClass == selectedUnitClass
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedUnitClass = uClass },
                                    label = { Text(uClass.titleRu, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DovahkiinCyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Squad Size Selection
                        Text("Численность отряда: $selectedSquadCount бойцов", color = ParchmentDark, fontSize = 11.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(10, 15, 20, 25, 30, 40, 50).forEach { count ->
                                val isSelected = count == selectedSquadCount
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSquadCount = count },
                                    label = { Text("$count", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldAccent,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dovahkiin leader checkbox
                        if (isDovahkiinHere) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = sendWithDovahkiin,
                                    onCheckedChange = { sendWithDovahkiin = it },
                                    colors = CheckboxDefaults.colors(checkedColor = DovahkiinCyan)
                                )
                                Text("Возглавить отряд Довакином (бонус Ту'ума)", color = DovahkiinCyan, fontSize = 11.sp)
                            }
                        }

                        // Resource cost calculation
                        val costGold = selectedUnitClass.costGold * (selectedSquadCount / 10).coerceAtLeast(1)
                        val costOre = selectedUnitClass.costOre * (selectedSquadCount / 10).coerceAtLeast(1)
                        val costRecruits = selectedSquadCount
                        val canDispatch = gameState.gold >= costGold && gameState.ore >= costOre && gameState.recruits >= costRecruits

                        Text(
                            text = "Стоимость: $costGold золота, $costOre руды, $costRecruits рекрутов",
                            color = if (canDispatch) GoldAccent else AlertFire,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dispatch button & Cancel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isDispatching = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Отмена", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    onDispatchSquad(
                                        location.id,
                                        selectedTargetId,
                                        selectedUnitClass,
                                        selectedSquadCount,
                                        sendWithDovahkiin
                                    )
                                    isDispatching = false
                                },
                                enabled = canDispatch,
                                modifier = Modifier
                                    .weight(2f)
                                    .testTag("confirm_dispatch_squad_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    disabledContainerColor = SkyrimBorder
                                )
                            ) {
                                Text(
                                    text = if (canDispatch) "Выступить в поход!" else "Недостаточно ресурсов",
                                    color = if (canDispatch) Color.Black else ParchmentDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Hostile location: Dedicated assault panel allowing multi-base convergence!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SkyrimSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = AlertFire, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ОРГАНИЗОВАТЬ ШТУРМ ЭТОЙ ЦИТАДЕЛИ",
                            color = AlertFire,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (connectedFriendlyBases.isEmpty()) {
                        Text(
                            text = "Нет прямых соседних подконтрольных крепостей. Захватите ближайший форт на тракте, чтобы открыть путь к осаде.",
                            color = ParchmentDark,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "Доступно баз для наступления: ${connectedFriendlyBases.size}. При атаке с нескольких баз силы отрядов суммируются в единую осадную армию!",
                            color = ParchmentLight,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Unit Class Selection for assault
                        Text("Род войск наступления:", color = ParchmentDark, fontSize = 11.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(UnitClass.RECON, UnitClass.INFANTRY, UnitClass.ARCHERS, UnitClass.MAGES, UnitClass.ELITE).forEach { uClass ->
                                val isSelected = uClass == selectedUnitClass
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedUnitClass = uClass },
                                    label = { Text(uClass.titleRu, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AlertFire,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val costSingleGold = selectedUnitClass.costGold * 2
                        val costSingleOre = selectedUnitClass.costOre * 2
                        val canAffordSingle = gameState.gold >= costSingleGold && gameState.ore >= costSingleOre && gameState.recruits >= 20

                        // Multi-base simultaneous assault hero button!
                        if (connectedFriendlyBases.size > 1) {
                            Button(
                                onClick = {
                                    val baseIds = connectedFriendlyBases.map { it.id }
                                    onDispatchMassAssault(baseIds, location.id, selectedUnitClass, 20)
                                    onClose()
                                },
                                enabled = canAffordSingle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mass_assault_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AlertFire,
                                    disabledContainerColor = SkyrimBorder
                                )
                            ) {
                                Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = if (canAffordSingle) Color.White else ParchmentDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (canAffordSingle) "⚔️ ШТУРМ СО ВСЕХ ${connectedFriendlyBases.size} БАЗ (СУММА СИЛ)" else "Недостаточно ресурсов для штурма",
                                    color = if (canAffordSingle) Color.White else ParchmentDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text("Или направить отряд из отдельной базы (${costSingleGold}G, ${costSingleOre} руды, 20 рекр.):", color = ParchmentDark, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        // List of individual bases with 1-tap dispatch
                        for (base in connectedFriendlyBases) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(base.nameRu, color = ParchmentLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Гарнизон: ${base.garrison} воинов", color = ParchmentDark, fontSize = 10.sp)
                                }
                                Button(
                                    onClick = {
                                        onDispatchSquad(base.id, location.id, selectedUnitClass, 20, false)
                                    },
                                    enabled = canAffordSingle,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldAccent,
                                        disabledContainerColor = SkyrimBorder
                                    ),
                                    modifier = Modifier.testTag("dispatch_from_${base.id}_button")
                                ) {
                                    Text(
                                        text = if (canAffordSingle) "Направить (+20)" else "Мало рес.",
                                        color = if (canAffordSingle) Color.Black else ParchmentDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
