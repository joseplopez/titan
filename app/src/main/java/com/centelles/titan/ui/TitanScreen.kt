package com.centelles.titan.ui

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.centelles.titan.logic.GameEvent
import com.centelles.titan.logic.GameViewModel
import com.centelles.titan.ui.components.ArcaneButton
import com.centelles.titan.ui.components.ArcanePanel
import com.centelles.titan.ui.theme.*
import com.centelles.titan.util.NumberFormatter
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random as KotlinRandom

import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitanScreen(viewModel: GameViewModel, onNavigateToUpgrades: () -> Unit, onNavigateToConstellation: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val boxSizePx = with(density) { 280.dp.toPx() }
    val halfBoxSizePx = boxSizePx / 2
    val groundAreaWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() - 32.dp.toPx() }

    // High-precision clock for all animations
    val animTime by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            withFrameMillis { value = it }
        }
    }
    
    // Entity Lists
    val damageNumbers = remember { mutableStateListOf<DamageNumber>() }
    val particles = remember { mutableStateListOf<ShardParticle>() }
    val projectiles = remember { mutableStateListOf<StrikerProjectile>() }

    // Origin Sync Logic
    fun getStrikerOffset(index: Int): Offset {
        val rotationSpeed = 3000 + index * 500
        val rotation = ( (animTime % rotationSpeed).toFloat() / rotationSpeed) * 360f
        val r = halfBoxSizePx * 0.9f + (index * 5f)
        return Offset(
            x = cos(Math.toRadians(rotation.toDouble())).toFloat() * r,
            y = sin(Math.toRadians(rotation.toDouble())).toFloat() * r
        )
    }

    // Interaction handling
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tapAnimatable = remember { Animatable(1f) }
    val flashAlpha = remember { Animatable(0f) }
    val shakeOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.StrikerHit -> {
                    val startPos = getStrikerOffset(event.strikerIndex)
                    // Random target offset from center of heart
                    val targetX = (KotlinRandom.nextFloat() * 120f) - 60f
                    val targetY = (KotlinRandom.nextFloat() * 120f) - 60f
                    projectiles.add(StrikerProjectile(System.nanoTime(), startPos, Offset(targetX, targetY), event.damage))
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(state.titanHp) {
        tapAnimatable.animateTo(0.95f, animationSpec = tween(50))
        tapAnimatable.animateTo(1f, animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f))
    }

    LaunchedEffect(state.currentRunShardsEarned) {
        flashAlpha.animateTo(0.4f, animationSpec = tween(50))
        flashAlpha.animateTo(0f, animationSpec = tween(200))
        shakeOffset.animateTo(Offset(KotlinRandom.nextFloat() * 10 - 5, KotlinRandom.nextFloat() * 10 - 5), animationSpec = tween(50))
        shakeOffset.animateTo(Offset.Zero, animationSpec = spring(stiffness = Spring.StiffnessHigh))
    }

    // Thorn Collision Detection
    LaunchedEffect(animTime) {
        if (state.thornSprites > 0 && particles.isNotEmpty()) {
            val visibleThorns = min(state.thornSprites, 5)
            // Height of GroundArea is 80dp
            // Spacer before GroundArea is 16dp
            // Half-height of Titan Box is 140dp
            // distance from center of heart to top of GroundArea = 140dp + 16dp = 156dp
            val groundAreaTopFromCenter = with(density) { 156.dp.toPx() }
            val groundAreaHeight = with(density) { 80.dp.toPx() }
            val fallDistance = with(density) { 400.dp.toPx() }
            
            val thorns = (0 until visibleThorns).map { i ->
                val patrolWidth = groundAreaWidthPx * 0.85f
                val hSpeed1 = 0.0012f + (i * 0.0001f)
                val hSpeed2 = 0.0007f + (i * 0.00005f)
                val vSpeed1 = 0.0009f + (i * 0.00008f)
                val vSpeed2 = 0.0005f + (i * 0.00004f)
                
                val hPhase1 = i * 2.1f
                val hPhase2 = i * 0.7f
                val vPhase1 = i * 1.3f
                val vPhase2 = i * 0.4f
                
                val x = (sin(animTime * hSpeed1 + hPhase1) * 0.7f + sin(animTime * hSpeed2 + hPhase2) * 0.3f) * patrolWidth / 2
                val yInGround = groundAreaHeight * (0.5f + (sin(animTime * vSpeed1 + vPhase1) * 0.7f + sin(animTime * vSpeed2 + vPhase2) * 0.3f) * 0.4f)
                val yRelativetoHeart = groundAreaTopFromCenter + yInGround
                Offset(x.toFloat(), yRelativetoHeart.toFloat())
            }

            val toRemove = mutableListOf<ShardParticle>()
            val currentParticles = particles.toList()
            currentParticles.forEach { p ->
                val t = animTime - p.startTime
                val progress = (t.toFloat() / 800f).coerceIn(0f, 1f) // Matches new 800ms duration
                val currentX = p.centerRelativePos.x + (p.randomX * progress)
                val currentY = p.centerRelativePos.y + (progress * fallDistance)
                val pPos = Offset(currentX, currentY)
                
                // Reduced radius for less aggressive collection
                if (thorns.any { tPos -> (pPos - tPos).getDistance() < 50f }) {
                    toRemove.add(p)
                }
            }
            
            if (toRemove.isNotEmpty()) {
                particles.removeAll(toRemove)
            }
        }
    }

    Scaffold(
        containerColor = VoidIndigo,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MoonMist),
                title = { Text("Titan's Heart", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToConstellation) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = EmberGold)
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                        ArcanePanel(modifier = Modifier.clickable { onNavigateToConstellation() }) {
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("${String.format(Locale.US, "%.1f", state.starlight)}⭐", color = SpectralCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Stars", fontSize = 8.sp, color = MoonMist.copy(alpha = 0.7f))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ArcanePanel {
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(NumberFormatter.format(state.shardsBanked), color = EmberGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("Shards", fontSize = 10.sp, color = MoonMist.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Starfield()

            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("DPS", NumberFormatter.format(state.totalDps))
                    StatItem("CPS", NumberFormatter.format(state.totalCps))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(280.dp)
                                .graphicsLayer(
                                    scaleX = tapAnimatable.value,
                                    scaleY = tapAnimatable.value,
                                    translationX = shakeOffset.value.x,
                                    translationY = shakeOffset.value.y
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        viewModel.onTitanTap()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        // Convert tap offset (absolute in box) to center-relative
                                        val centerRelativePos = offset - Offset(halfBoxSizePx, halfBoxSizePx)
                                        damageNumbers.add(DamageNumber(System.nanoTime(), state.clickDamage, centerRelativePos))
                                        repeat(3) { 
                                            particles.add(ShardParticle(
                                                id = System.nanoTime() + it, 
                                                centerRelativePos = centerRelativePos,
                                                randomX = KotlinRandom.nextFloat() * 160 - 80,
                                                randomY = KotlinRandom.nextFloat() * 100,
                                                startTime = animTime
                                            )) 
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            TitanCanvas(
                                hpFraction = (state.titanHp / state.maxTitanHp).toFloat(),
                                flashAlpha = flashAlpha.value,
                                isBrittle = state.frostSprites > 0,
                                isBurning = state.emberSprites > 0,
                                layer = state.currentLayer
                            )
                            
                            // Sprite Layer
                            SpriteMotes(state, boxSizePx, animTime)
                            
                            // Projectile Layer
                            projectiles.forEach { proj -> 
                                key(proj.id) { 
                                    StrikerProjectileMote(proj) { 
                                        projectiles.remove(proj)
                                        damageNumbers.add(DamageNumber(System.nanoTime(), proj.damage, proj.target))
                                        repeat(2) { 
                                            particles.add(ShardParticle(
                                                id = System.nanoTime() + it, 
                                                centerRelativePos = proj.target,
                                                randomX = KotlinRandom.nextFloat() * 160 - 80,
                                                randomY = KotlinRandom.nextFloat() * 100,
                                                startTime = animTime
                                            )) 
                                        } 
                                    } 
                                } 
                            }

                            // Weakspots (Interactive Layer)
                            state.activeCracks.forEach { crack ->
                                Weakspot(
                                    crack = crack,
                                    boxSizePx = boxSizePx,
                                    onTap = { 
                                        viewModel.onCrackTap(crack.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Normalized to center-relative
                                        val centerRelativePos = Offset((crack.x - 0.5f) * boxSizePx, (crack.y - 0.5f) * boxSizePx)
                                        damageNumbers.add(DamageNumber(System.nanoTime(), state.clickDamage * 5, centerRelativePos, isCrit = true))
                                        repeat(8) { 
                                            particles.add(ShardParticle(
                                                id = System.nanoTime() + it, 
                                                centerRelativePos = centerRelativePos,
                                                randomX = KotlinRandom.nextFloat() * 160 - 80,
                                                randomY = KotlinRandom.nextFloat() * 100,
                                                startTime = animTime
                                            )) 
                                        }
                                    }
                                )
                            }
                        }
                        
                        GroundArea(state, animTime, modifier = Modifier.fillMaxWidth().height(80.dp))
                    }
                    
                    // Feedback Layer (Unclipped)
                    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                        damageNumbers.forEach { dn -> key(dn.id) { FloatingDamageText(dn) { damageNumbers.remove(dn) } } }
                        particles.forEach { p -> key(p.id) { ShardParticleMote(p) { particles.remove(p) } } }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Stage ${state.awakeningStage + 1}", style = MaterialTheme.typography.labelLarge, color = EmberGold, modifier = Modifier.padding(bottom = 8.dp))

                    val hpProgress = (state.titanHp / state.maxTitanHp).toFloat()
                    val animatedHp by animateFloatAsState(targetValue = hpProgress, label = "hpProgress")
                    
                    val hpColor = when(state.currentLayer) {
                        2 -> Color.White
                        3 -> Color.Red
                        4 -> Color.Green
                        else -> SpectralCyan
                    }
                    
                    Box(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                        LinearProgressIndicator(
                            progress = { animatedHp },
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            color = if (state.emberSprites > 0) CrackMagma else hpColor,
                            trackColor = MysticBlue.copy(alpha = 0.3f)
                        )
                    }
                    Text("${NumberFormatter.format(state.titanHp)} / ${NumberFormatter.format(state.maxTitanHp)}", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelSmall, color = MoonMist)
                }

                Spacer(modifier = Modifier.weight(0.1f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArcaneButton(onClick = onNavigateToUpgrades, modifier = Modifier.weight(1f), containerColor = MysticBlue) { Text("Upgrades", style = MaterialTheme.typography.labelSmall) }
                    
                    val canDescend = state.canDescend()
                    ArcaneButton(
                        onClick = { if (canDescend) viewModel.onDescend() },
                        modifier = Modifier.weight(1.2f),
                        containerColor = if (canDescend) ArcanePurple else Color.Gray.copy(alpha = 0.5f),
                        enabled = true
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (canDescend) "The Descent" else "Reach Stage ${state.currentLayerDef.finalStage}",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                            if (canDescend) {
                                Text("+${String.format(Locale.US, "%.1f", state.calculateStarlightReward())}⭐", fontSize = 10.sp)
                            }
                        }
                    }

                    ArcaneButton(onClick = { viewModel.onManualCollect(); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, modifier = Modifier.weight(1f), containerColor = EmberGold) { Text("Sweep", style = MaterialTheme.typography.labelSmall) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ArcanePanel(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Recruit Sprites (${state.currentSpriteCount}/${state.spriteCapacity})", style = MaterialTheme.typography.labelSmall, color = MoonMist)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RecruitButton("Striker", state.strikersCount, state.getStrikerCost(), state.shardsBanked >= state.getStrikerCost() && state.currentSpriteCount < state.spriteCapacity, SpectralCyan) { viewModel.recruitStriker() }
                                RecruitButton("Gatherer", state.gatherersCount, state.getGathererCost(), state.shardsBanked >= state.getGathererCost() && state.currentSpriteCount < state.spriteCapacity, Color(0xFF4CAF50)) { viewModel.recruitGatherer() }
                            }
                            if (state.isUpgradeUnlocked("unlock_ember") || state.isUpgradeUnlocked("unlock_frost") || state.isUpgradeUnlocked("unlock_thorn")) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (state.isUpgradeUnlocked("unlock_ember")) RecruitButton("Ember", state.emberSprites, state.getEmberCost(), state.shardsBanked >= state.getEmberCost() && state.currentSpriteCount < state.spriteCapacity, Color(0xFFFF5722)) { viewModel.recruitEmber() }
                                    if (state.isUpgradeUnlocked("unlock_frost")) RecruitButton("Frost", state.frostSprites, state.getFrostCost(), state.shardsBanked >= state.getFrostCost() && state.currentSpriteCount < state.spriteCapacity, Color(0xFF03A9F4)) { viewModel.recruitFrost() }
                                }
                                if (state.isUpgradeUnlocked("unlock_thorn")) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        RecruitButton("Thorn", state.thornSprites, state.getThornCost(), state.shardsBanked >= state.getThornCost() && state.currentSpriteCount < state.spriteCapacity, Color(0xFFFFC107)) { viewModel.recruitThorn() }
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpriteMotes(state: com.centelles.titan.logic.GameState, boxSizePx: Float, animTime: Long) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        
        // Strikers
        val visibleStrikers = min(state.strikersCount, 10)
        repeat(visibleStrikers) { i ->
            val rotationSpeed = 3000 + i * 500
            val angle = ( (animTime % rotationSpeed).toFloat() / rotationSpeed) * 360f
            val r = (size.width / 2) * 0.9f + (i * 5f)
            val pos = center + Offset(cos(Math.toRadians(angle.toDouble())).toFloat() * r, sin(Math.toRadians(angle.toDouble())).toFloat() * r)
            drawCircle(brush = Brush.radialGradient(colors = listOf(SpectralCyan, Color.Transparent), center = pos, radius = 10f), center = pos, radius = 10f)
            drawCircle(color = SpectralCyan, radius = 3f, center = pos)
        }
    }
}

@Composable
fun StrikerProjectileMote(proj: StrikerProjectile, onHit: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(400, easing = FastOutLinearInEasing))
        onHit()
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val startAbs = center + proj.start
        val targetAbs = center + proj.target
        val currentPos = startAbs + (targetAbs - startAbs) * progress.value
        
        drawCircle(color = SpectralCyan, radius = 4f, center = currentPos)
        drawCircle(color = SpectralCyan.copy(alpha = 0.3f), radius = 10f, center = currentPos)
    }
}

@Composable
fun TitanCanvas(
    hpFraction: Float,
    flashAlpha: Float,
    isBrittle: Boolean,
    isBurning: Boolean,
    layer: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowIntensity by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 0.8f, animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "glowIntensity")
    
    val veinColor = when(layer) {
        2 -> Color.White
        3 -> Color.Red
        4 -> Color.Green
        else -> if (isBurning) CrackMagma else SpectralCyan
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width * 0.4f
        val path = Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius * 0.8f, center.y - radius * 0.5f)
            lineTo(center.x + radius * 0.9f, center.y + radius * 0.4f)
            lineTo(center.x, center.y + radius)
            lineTo(center.x - radius * 0.9f, center.y + radius * 0.4f)
            lineTo(center.x - radius * 0.8f, center.y - radius * 0.5f)
            close()
        }
        
        val stoneColors = when(layer) {
            2 -> listOf(MysticBlue, VoidIndigo)
            3 -> listOf(Color(0xFF3E2723), Color(0xFF212121))
            4 -> listOf(Color(0xFF1B5E20), Color(0xFF004D40))
            else -> listOf(MysticBlue, VoidIndigo)
        }

        drawPath(path = path, brush = Brush.radialGradient(colors = stoneColors, center = center, radius = radius * 1.5f))
        
        drawPath(path = path, color = veinColor.copy(alpha = 0.2f * glowIntensity))
        val cracksCount = ((1f - hpFraction) * 20).toInt()
        val random = java.util.Random(42)
        repeat(cracksCount) {
            val startAngle = random.nextFloat() * 360f
            val length = random.nextFloat() * radius * 0.6f
            val startX = center.x + cos(Math.toRadians(startAngle.toDouble())).toFloat() * radius * 0.2f
            val startY = center.y + sin(Math.toRadians(startAngle.toDouble())).toFloat() * radius * 0.2f
            drawLine(color = veinColor.copy(alpha = 0.6f), start = Offset(startX, startY), end = Offset(startX + cos(Math.toRadians(startAngle.toDouble())).toFloat() * length, startY + sin(Math.toRadians(startAngle.toDouble())).toFloat() * length), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        
        val outlineColor = when(layer) {
            2 -> SpectralCyan
            3 -> Color.Red
            4 -> EmberGold
            else -> if (isBrittle) Color(0xFFADD8E6) else MysticBlue
        }
        drawPath(path = path, color = outlineColor, style = Stroke(width = 2.dp.toPx()))
        if (flashAlpha > 0) drawPath(path = path, color = Color.White.copy(alpha = flashAlpha))
    }
}

@Composable
fun GroundArea(state: com.centelles.titan.logic.GameState, animTime: Long, modifier: Modifier = Modifier) {
    val shards = state.shardsOnGround
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Draw Shards (Rectangular shape, higher density)
            if (shards > 0) {
                val count = min(shards.toInt() + 20, 500)
                val random = java.util.Random(123)
                repeat(count) {
                    val x = random.nextFloat() * width
                    val y = random.nextFloat() * height
                    drawCircle(
                        color = EmberGold, 
                        radius = (1f + random.nextFloat() * 1.5f).dp.toPx(), 
                        center = Offset(x, y),
                        alpha = 0.4f + random.nextFloat() * 0.6f
                    )
                }
            }
            
            // Draw Gatherers (Complex random-looking paths)
            val visibleGatherers = min(state.gatherersCount, 12)
            repeat(visibleGatherers) { i ->
                val patrolWidth = width * 0.95f
                val hSpeed1 = 0.0008f + (i * 0.0001f)
                val hSpeed2 = 0.0005f + (i * 0.00007f)
                val vSpeed1 = 0.0005f + (i * 0.00012f)
                val vSpeed2 = 0.0003f + (i * 0.00006f)
                
                val hPhase1 = i * 0.8f
                val hPhase2 = i * 1.9f
                val vPhase1 = i * 1.4f
                val vPhase2 = i * 0.3f
                
                val x = (width / 2) + (sin(animTime * hSpeed1 + hPhase1) * 0.6f + sin(animTime * hSpeed2 + hPhase2) * 0.4f) * patrolWidth / 2
                val y = height * (0.5f + (sin(animTime * vSpeed1 + vPhase1) * 0.7f + sin(animTime * vSpeed2 + vPhase2) * 0.3f) * 0.4f)
                drawCircle(color = Color(0xFF4CAF50), radius = 3f, center = Offset(x.toFloat(), y.toFloat()))
            }
            
            // Draw Thorns (Complex random-looking paths)
            val visibleThorns = min(state.thornSprites, 8)
            repeat(visibleThorns) { i ->
                val patrolWidth = width * 0.9f
                val hSpeed1 = 0.0012f + (i * 0.0001f)
                val hSpeed2 = 0.0007f + (i * 0.00005f)
                val vSpeed1 = 0.0009f + (i * 0.00008f)
                val vSpeed2 = 0.0005f + (i * 0.00004f)
                
                val hPhase1 = i * 2.1f
                val hPhase2 = i * 0.7f
                val vPhase1 = i * 1.3f
                val vPhase2 = i * 0.4f
                
                val x = (width / 2) + (sin(animTime * hSpeed1 + hPhase1) * 0.7f + sin(animTime * hSpeed2 + hPhase2) * 0.3f) * patrolWidth / 2
                val y = height * (0.5f + (sin(animTime * vSpeed1 + vPhase1) * 0.6f + sin(animTime * vSpeed2 + vPhase2) * 0.4f) * 0.4f)
                drawCircle(color = Color(0xFFFFC107), radius = 5f, center = Offset(x.toFloat(), y.toFloat()))
            }
        }
    }
}

@Composable
fun Weakspot(crack: com.centelles.titan.logic.Crack, boxSizePx: Float, onTap: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "weakspotPulse")
    val pulseScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.3f, animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutLinearInEasing), repeatMode = RepeatMode.Restart), label = "pulseScale")
    val pulseAlpha by infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 0f, animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "pulseAlpha")
    val timerProgress = remember { mutableStateOf(1f) }
    LaunchedEffect(crack) {
        val duration = crack.expiryTime - System.currentTimeMillis()
        if (duration > 0) animate(initialValue = 1f, targetValue = 0f, animationSpec = tween(duration.toInt(), easing = LinearEasing)) { v, _ -> timerProgress.value = v }
    }
    Box(modifier = Modifier.size(48.dp).offset { IntOffset((crack.x * boxSizePx - boxSizePx/2).toInt(), (crack.y * boxSizePx - boxSizePx/2).toInt()) }.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = CrackMagma, radius = (size.width / 4) * pulseScale, alpha = pulseAlpha, style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = CrackMagma, radius = size.width / 6)
            drawArc(color = MoonMist.copy(alpha = 0.5f), startAngle = -90f, sweepAngle = 360f * timerProgress.value, useCenter = false, style = Stroke(width = 2.dp.toPx()), size = size.copy(width = size.width * 0.8f, height = size.height * 0.8f), topLeft = Offset(size.width * 0.1f, size.height * 0.1f))
        }
    }
}

// Draw a mote inside a Canvas relative to the Canvas center
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMote(color: Color, offset: Offset, size: Float = 6f) {
    drawCircle(brush = Brush.radialGradient(colors = listOf(color, Color.Transparent), center = center + offset, radius = size * 2), center = center + offset, radius = size * 2)
    drawCircle(color = color, radius = size / 2, center = center + offset)
}

data class DamageNumber(val id: Long, val amount: Double, val centerRelativePos: Offset, val isCrit: Boolean = false)
data class ShardParticle(val id: Long, val centerRelativePos: Offset, val randomX: Float, val randomY: Float, val startTime: Long)
data class StrikerProjectile(val id: Long, val start: Offset, val target: Offset, val damage: Double)

@Composable
fun FloatingDamageText(dn: DamageNumber, onAnimationEnd: () -> Unit) {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animatable.animateTo(1f, animationSpec = tween(800, easing = LinearOutSlowInEasing))
        onAnimationEnd()
    }
    // Origin is center of heart
    Text(
        text = NumberFormatter.format(dn.amount),
        color = if (dn.isCrit) CrackMagma else MoonMist,
        fontWeight = if (dn.isCrit) FontWeight.ExtraBold else FontWeight.Bold,
        fontSize = if (dn.isCrit) 20.sp else 16.sp,
        style = TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)),
        modifier = Modifier.offset { 
            IntOffset(
                dn.centerRelativePos.x.toInt() - 25, 
                dn.centerRelativePos.y.toInt() - 50 - (animatable.value * 150).toInt()
            ) 
        }
        .graphicsLayer(alpha = 1f - animatable.value)
        .zIndex(100f)
    )
}

@Composable
fun ShardParticleMote(p: ShardParticle, onAnimationEnd: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        onAnimationEnd()
    }
    // Origin is center of heart
    Canvas(modifier = Modifier.size(8.dp).offset {
        val targetXPx = p.centerRelativePos.x + (p.randomX * progress.value)
        val fallDistance = 400.dp.toPx()
        val targetYPx = p.centerRelativePos.y + (progress.value * fallDistance)
        IntOffset(targetXPx.toInt() - 4.dp.toPx().toInt(), targetYPx.toInt() - 4.dp.toPx().toInt())
    }.graphicsLayer(alpha = 1f - progress.value, scaleX = 1f - progress.value, scaleY = 1f - progress.value).zIndex(90f)) { drawCircle(color = EmberGold) }
}

@Composable
fun Starfield() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val starOffset by infiniteTransition.animateFloat(0f, 1000f, infiniteRepeatable(tween(60000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "")
    Canvas(modifier = Modifier.fillMaxSize()) {
        val random = java.util.Random(100)
        repeat(50) {
            val x = (random.nextFloat() * size.width + starOffset * 0.1f) % size.width
            val y = (random.nextFloat() * size.height + starOffset * 0.05f) % size.height
            drawCircle(color = MoonMist, radius = random.nextFloat() * 1.5.dp.toPx(), center = Offset(x, y), alpha = random.nextFloat() * 0.5f)
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MoonMist.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = SpectralCyan, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RowScope.RecruitButton(label: String, count: Int, cost: Double, enabled: Boolean, color: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    ArcaneButton(onClick = { onClick(); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, enabled = enabled, modifier = Modifier.weight(1f), containerColor = MysticBlue, contentPadding = PaddingValues(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text("(${NumberFormatter.format(cost)})", fontSize = 10.sp, color = EmberGold.copy(alpha = if (enabled) 1f else 0.5f))
            Text("x$count", fontSize = 10.sp, color = MoonMist.copy(alpha = 0.7f))
        }
    }
}
