package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun SkyrimMapView(
    gameState: GameState,
    onLocationSelected: (String) -> Unit,
    onOpenBattle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // High performance direct state variables for zero-latency 60 FPS drag & zoom
    var currentScale by remember { mutableFloatStateOf(1.05f) }
    var currentOffsetX by remember { mutableFloatStateOf(-120f) }
    var currentOffsetY by remember { mutableFloatStateOf(-80f) }

    // Animatable exclusively for smooth cinematic focus transitions when tapping a node
    val animFocusX = remember { Animatable(0f) }
    val animFocusY = remember { Animatable(0f) }
    var isFocusing by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "map_effects")
    val siegePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "siege_pulse"
    )

    val marchPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "march_pulse"
    )

    val textMeasurer = rememberTextMeasurer()
    val parchmentBitmap = ImageBitmap.imageResource(id = R.drawable.skyrim_parchment_map_1791789060938)

    // Pre-indexed location map for O(1) instant lookups
    val locationMap = remember(gameState.locations) {
        gameState.locations.associateBy { it.id }
    }

    // Pre-computed road network geometry computed once for lifetime of map (static geography)
    data class RoadLine(val fromId: String, val toId: String, val start: Offset, val end: Offset)
    val roadLines = remember {
        val initialLocMap = SkyrimLocationsData.initialLocations.associateBy { it.id }
        val list = mutableListOf<RoadLine>()
        val visited = mutableSetOf<Pair<String, String>>()
        for (loc in SkyrimLocationsData.initialLocations) {
            val p1 = Offset(loc.x, loc.y)
            for (connId in loc.connections) {
                val pair = if (loc.id < connId) loc.id to connId else connId to loc.id
                if (!visited.contains(pair)) {
                    visited.add(pair)
                    val neighbor = initialLocMap[connId]
                    if (neighbor != null) {
                        list.add(RoadLine(loc.id, connId, p1, Offset(neighbor.x, neighbor.y)))
                    }
                }
            }
        }
        list
    }

    // Pre-computed short names once
    val shortNames = remember {
        SkyrimLocationsData.initialLocations.associate { it.id to it.nameRu.split(" ").first() }
    }

    // Cached TextLayoutResult to eliminate ~2,100 expensive text layouts per second
    val nodeTextCache = remember { mutableMapOf<String, TextLayoutResult>() }
    val squadTextCache = remember { mutableMapOf<String, TextLayoutResult>() }

    // Cached dash effects by quantized scale factor to eliminate floatArray and JNI PathEffect churn
    val dashEffectCache = remember { mutableMapOf<Int, Pair<PathEffect, PathEffect>>() }

    val activeOffsetX = if (isFocusing) animFocusX.value else currentOffsetX
    val activeOffsetY = if (isFocusing) animFocusY.value else currentOffsetY

    // Read updated values inside pointerInput without restarting gesture detector on every pan
    val currentLocationsState by rememberUpdatedState(gameState.locations)
    val currentScaleState by rememberUpdatedState(currentScale)
    val currentOffsetXState by rememberUpdatedState(activeOffsetX)
    val currentOffsetYState by rememberUpdatedState(activeOffsetY)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SkyrimDarkBackground)
            .testTag("skyrim_map_canvas_container")
    ) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        isFocusing = false
                        currentScale = (currentScale * zoom).coerceIn(0.55f, 3.5f)
                        currentOffsetX += pan.x
                        currentOffsetY += pan.y
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val curScale = currentScaleState
                        val curOffX = currentOffsetXState
                        val curOffY = currentOffsetYState
                        val mapX = (tapOffset.x - curOffX) / curScale
                        val mapY = (tapOffset.y - curOffY) / curScale

                        val clicked = currentLocationsState.minByOrNull { loc ->
                            hypot(loc.x - mapX, loc.y - mapY)
                        }

                        if (clicked != null && hypot(clicked.x - mapX, clicked.y - mapY) < 55f) {
                            onLocationSelected(clicked.id)

                            val targetOffX = (viewWidth / 2f) - (clicked.x * curScale)
                            val targetOffY = (viewHeight / 2.3f) - (clicked.y * curScale)

                            coroutineScope.launch {
                                isFocusing = true
                                animFocusX.snapTo(curOffX)
                                animFocusY.snapTo(curOffY)
                                launch { animFocusX.animateTo(targetOffX, tween(300, easing = FastOutSlowInEasing)) }
                                launch { animFocusY.animateTo(targetOffY, tween(300, easing = FastOutSlowInEasing)) }
                                currentOffsetX = targetOffX
                                currentOffsetY = targetOffY
                                isFocusing = false
                            }
                        }
                    }
                }
        ) {
            val scale = currentScale
            val offX = activeOffsetX
            val offY = activeOffsetY

            // 1. Draw Authentic Skyrim Parchment Map (1200 x 896 native dimensions)
            drawImage(
                image = parchmentBitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(parchmentBitmap.width, parchmentBitmap.height),
                dstOffset = IntOffset(offX.toInt(), offY.toInt()),
                dstSize = IntSize((parchmentBitmap.width.toFloat() * scale).toInt(), (parchmentBitmap.height.toFloat() * scale).toInt()),
                alpha = 0.95f
            )

            val mapTransform: (Offset) -> Offset = { pt ->
                Offset(pt.x * scale + offX, pt.y * scale + offY)
            }

            // 2. Road Network (using precomputed roadLines and cached DashPathEffect)
            val scaleKey = (scale * 10f).toInt()
            val (roadDashEffect, squadDashEffect) = dashEffectCache.getOrPut(scaleKey) {
                val s = scaleKey / 10f
                Pair(
                    PathEffect.dashPathEffect(floatArrayOf(10f * s, 6f * s)),
                    PathEffect.dashPathEffect(floatArrayOf(6f * s, 4f * s))
                )
            }

            for (road in roadLines) {
                val p1 = mapTransform(road.start)
                val p2 = mapTransform(road.end)
                val fromLoc = locationMap[road.fromId]
                val toLoc = locationMap[road.toId]
                val roadColor = if (fromLoc != null && toLoc != null && fromLoc.ownerFactionId == toLoc.ownerFactionId) {
                    FactionsData.getFaction(fromLoc.ownerFactionId).primaryColor.copy(alpha = 0.55f)
                } else {
                    RoadBrown.copy(alpha = 0.55f)
                }

                drawLine(
                    color = roadColor,
                    start = p1,
                    end = p2,
                    strokeWidth = 3f * scale,
                    pathEffect = roadDashEffect
                )
            }

            // 3. Marching Squads (Multi-squad converging indicators)
            for (squad in gameState.squads) {
                val fromLoc = locationMap[squad.fromLocationId]
                val toLoc = locationMap[squad.targetLocationId]
                if (fromLoc != null && toLoc != null) {
                    val currentX = fromLoc.x + (toLoc.x - fromLoc.x) * squad.progress
                    val currentY = fromLoc.y + (toLoc.y - fromLoc.y) * squad.progress
                    val squadPos = mapTransform(Offset(currentX, currentY))

                    val squadColor = FactionsData.getFaction(squad.factionId).primaryColor

                    // March trail vector towards target
                    val targetPos = mapTransform(Offset(toLoc.x, toLoc.y))
                    drawLine(
                        color = squadColor.copy(alpha = 0.45f),
                        start = squadPos,
                        end = targetPos,
                        strokeWidth = 2f * scale,
                        pathEffect = squadDashEffect
                    )

                    drawCircle(
                        color = squadColor.copy(alpha = 0.38f),
                        radius = (14f * scale) * marchPulse,
                        center = squadPos
                    )
                    drawCircle(
                        color = squadColor,
                        radius = 9f * scale,
                        center = squadPos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 9f * scale,
                        center = squadPos,
                        style = Stroke(width = 2f * scale)
                    )

                    val squadLabel = if (squad.isDovahkiinSquad) "ДОВАКИН" else "${squad.count}"
                    val squadFontSp = (9f * scale).toInt().coerceIn(7, 13)
                    val squadCacheKey = "${squadLabel}_$squadFontSp"
                    val textResult = squadTextCache.getOrPut(squadCacheKey) {
                        textMeasurer.measure(
                            text = AnnotatedString(squadLabel),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = squadFontSp.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset(squadPos.x - textResult.size.width / 2, squadPos.y - 20f * scale)
                    )
                }
            }

            // 4. Strategic Location Nodes
            for (node in gameState.locations) {
                val nodePos = mapTransform(Offset(node.x, node.y))
                val faction = FactionsData.getFaction(node.ownerFactionId)
                val isSelected = node.id == gameState.selectedLocationId
                val isUnderSiege = node.isUnderSiege

                val baseRadius = when (node.type) {
                    LocationType.CITY -> 18f * scale
                    LocationType.FORT, LocationType.STRONGHOLD -> 14f * scale
                    LocationType.MINE -> 12f * scale
                    LocationType.SETTLEMENT -> 10f * scale
                }

                if (isUnderSiege) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AlertFire.copy(alpha = 0.85f), Color.Transparent),
                            center = nodePos,
                            radius = (baseRadius * 2.3f) * siegePulse
                        ),
                        radius = (baseRadius * 2.3f) * siegePulse,
                        center = nodePos
                    )
                }

                if (isSelected) {
                    drawCircle(
                        color = GoldAccent.copy(alpha = 0.45f),
                        radius = baseRadius + 8f * scale,
                        center = nodePos
                    )
                }

                drawCircle(
                    color = if (isSelected) GoldAccent else SkyrimDarkBackground,
                    radius = baseRadius + 3f * scale,
                    center = nodePos
                )

                drawCircle(
                    color = faction.primaryColor,
                    radius = baseRadius,
                    center = nodePos
                )

                when (node.type) {
                    LocationType.CITY -> {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.95f),
                            radius = baseRadius * 0.45f,
                            center = nodePos
                        )
                    }
                    LocationType.FORT, LocationType.STRONGHOLD -> {
                        drawRect(
                            color = Color.White.copy(alpha = 0.95f),
                            topLeft = Offset(nodePos.x - baseRadius * 0.35f, nodePos.y - baseRadius * 0.35f),
                            size = androidx.compose.ui.geometry.Size(baseRadius * 0.7f, baseRadius * 0.7f)
                        )
                    }
                    LocationType.MINE -> {
                        drawCircle(
                            color = GoldAccent.copy(alpha = 0.95f),
                            radius = baseRadius * 0.35f,
                            center = nodePos
                        )
                    }
                    LocationType.SETTLEMENT -> {
                        drawCircle(
                            color = ParchmentLight.copy(alpha = 0.9f),
                            radius = baseRadius * 0.3f,
                            center = nodePos
                        )
                    }
                }

                // Name label below node (cached in nodeTextCache to eliminate thousands of text measures per second)
                val shortName = shortNames[node.id] ?: node.nameRu.substringBefore(" ")
                val fontSp = (10f * scale).toInt().coerceIn(8, 15)
                val cacheKey = "${node.id}_${isSelected}_$fontSp"
                val labelLayout = nodeTextCache.getOrPut(cacheKey) {
                    textMeasurer.measure(
                        text = AnnotatedString(shortName),
                        style = TextStyle(
                            color = if (isSelected) GoldAccent else ParchmentLight,
                            fontSize = fontSp.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            shadow = Shadow(color = Color.Black, blurRadius = 4f)
                        )
                    )
                }
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(nodePos.x - labelLayout.size.width / 2, nodePos.y + baseRadius + 4f * scale)
                )
            }
        }

        // Floating Map Controls for Mobile (Zoom In/Out, Center on Capital, Jump to Battle)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .testTag("map_floating_controls"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Jump to Battle button (only if player's faction is actively fighting!)
            val playerActiveBattle = gameState.activeBattles.firstOrNull { it.isPlayerInvolved(gameState.playerFactionId) }
            if (playerActiveBattle != null) {
                val battleLoc = gameState.locations.firstOrNull { it.id == playerActiveBattle.locationId }
                if (battleLoc != null) {
                    SmallFloatingActionButton(
                        onClick = {
                            onOpenBattle(battleLoc.id)
                            val targetOffX = (viewWidth / 2f) - (battleLoc.x * currentScale)
                            val targetOffY = (viewHeight / 2.3f) - (battleLoc.y * currentScale)
                            coroutineScope.launch {
                                isFocusing = true
                                animFocusX.snapTo(activeOffsetX)
                                animFocusY.snapTo(activeOffsetY)
                                launch { animFocusX.animateTo(targetOffX, tween(300, easing = FastOutSlowInEasing)) }
                                launch { animFocusY.animateTo(targetOffY, tween(300, easing = FastOutSlowInEasing)) }
                                currentOffsetX = targetOffX
                                currentOffsetY = targetOffY
                                isFocusing = false
                            }
                        },
                        containerColor = AlertFire,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("jump_to_battle_fab")
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = "К битве")
                    }
                }
            }

            // Zoom In button
            SmallFloatingActionButton(
                onClick = {
                    currentScale = (currentScale * 1.25f).coerceIn(0.55f, 3.5f)
                },
                containerColor = SkyrimSurfaceVariant,
                contentColor = ParchmentLight,
                modifier = Modifier.testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Приблизить")
            }

            // Zoom Out button
            SmallFloatingActionButton(
                onClick = {
                    currentScale = (currentScale / 1.25f).coerceIn(0.55f, 3.5f)
                },
                containerColor = SkyrimSurfaceVariant,
                contentColor = ParchmentLight,
                modifier = Modifier.testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Отдалить")
            }

            // Reset View / Center on Capital or Owned Base
            SmallFloatingActionButton(
                onClick = {
                    val capital = gameState.locations.firstOrNull { it.ownerFactionId == gameState.playerFactionId && it.type == LocationType.CITY }
                        ?: gameState.locations.firstOrNull { it.ownerFactionId == gameState.playerFactionId }
                        ?: gameState.locations.firstOrNull()

                    if (capital != null) {
                        val targetOffX = (viewWidth / 2f) - (capital.x * 1.05f)
                        val targetOffY = (viewHeight / 2.3f) - (capital.y * 1.05f)
                        coroutineScope.launch {
                            isFocusing = true
                            animFocusX.snapTo(activeOffsetX)
                            animFocusY.snapTo(activeOffsetY)
                            launch { animFocusX.animateTo(targetOffX, tween(350, easing = FastOutSlowInEasing)) }
                            launch { animFocusY.animateTo(targetOffY, tween(350, easing = FastOutSlowInEasing)) }
                            currentScale = 1.05f
                            currentOffsetX = targetOffX
                            currentOffsetY = targetOffY
                            isFocusing = false
                        }
                    }
                },
                containerColor = SkyrimSurfaceVariant,
                contentColor = GoldAccent,
                modifier = Modifier.testTag("center_capital_button")
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Центр")
            }
        }
    }
}
