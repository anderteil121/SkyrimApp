package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GameState
import com.example.data.model.MapVisualMode
import com.example.ui.theme.*

@Composable
fun MapSourceDialog(
    gameState: GameState,
    onToggleVisualMode: (MapVisualMode) -> Unit,
    onLoadExpandedPreset: () -> Unit,
    onResetDefaultMap: () -> Unit,
    onParseFromUrl: (String) -> Unit,
    isParsing: Boolean,
    parseStatusMessage: String?,
    onClose: () -> Unit
) {
    var customUrlInput by remember { mutableStateOf("https://raw.githubusercontent.com/datasets/geo-boundaries-world-110m/master/countries.geojson") }
    var showUrlField by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("map_source_dialog"),
            color = SkyrimSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent),
            tonalElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Настройки карты и парсера", color = ParchmentLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = ParchmentDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Map Source Info
                Surface(
                    color = SkyrimSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = DovahkiinCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Активная карта:", color = ParchmentDark, fontSize = 11.sp)
                            Text(gameState.mapSourceDescription, color = ParchmentLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Visual Map Mode (Parchment vs Tactical)
                Text("Визуальный стиль карты:", color = ParchmentDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MapVisualMode.values().forEach { mode ->
                        val isSelected = mode == gameState.mapVisualMode
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleVisualMode(mode) },
                            label = { Text(if (mode == MapVisualMode.AUTHENTIC_PARCHMENT) "Пергамент" else "Планшет", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    if (mode == MapVisualMode.AUTHENTIC_PARCHMENT) Icons.Default.Landscape else Icons.Default.GridOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Map Data Presets & Parser
                Text("Источник геоданных (Парсинг точек):", color = ParchmentDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                // Preset 1: Standard 32 nodes
                OutlinedButton(
                    onClick = {
                        onResetDefaultMap()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorderLight)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Стандартная карта (32 узла, оффлайн)", fontSize = 12.sp, color = ParchmentLight)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Preset 2: Expanded with Labyrinthian, Pale Pass, etc.
                OutlinedButton(
                    onClick = {
                        onLoadExpandedPreset()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyrimBorderLight)
                ) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Расширенный Скайрим (+Лабиринтиан, Проход)", fontSize = 12.sp, color = ParchmentLight)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Option 3: Parse from custom URL / GeoJSON
                Button(
                    onClick = { showUrlField = !showUrlField },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyrimSurfaceVariant)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = DovahkiinCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Спарсить с внешнего URL (JSON / GeoJSON)", color = DovahkiinCyan, fontSize = 12.sp)
                }

                if (showUrlField) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("URL с GeoJSON / JSON картой", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = SkyrimBorder,
                            focusedTextColor = ParchmentLight,
                            unfocusedTextColor = ParchmentLight
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { onParseFromUrl(customUrlInput) },
                        enabled = !isParsing && customUrlInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        if (isParsing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Парсинг данных...", color = Color.Black, fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Загрузить и спарсить точки", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (parseStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = parseStatusMessage,
                        color = if (parseStatusMessage.startsWith("Ошибка")) AlertFire else HealthGreen,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
