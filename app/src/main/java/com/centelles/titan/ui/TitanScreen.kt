package com.centelles.titan.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random as KotlinRandom
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitanScreen(viewModel: GameViewModel, onNavigateToUpgrades: () -> Unit, onNavigateToConstellation: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val boxSizePx = with(density) { 280.dp.toPx() }
    val halfBoxSizePx = boxSizePx / 2
    
    // Visual Entities
    val damageNumbers = remember { mutableStateListOf<DamageNumber>() }
    val particles = remember { mutableStateListOf<ShardParticle>() }
    val projectiles = remember { mutableStateListOf<StrikerProjectile>() }
    
    // Scale animation for tapping
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val idleScale by rememberInfiniteTransition(label = "idleScale").animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleScale"
    )

    val tapAnimatable = remember { Animatable(1f) }
    val flashAlpha = remember { Animatable(0f) }
    val shakeOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    // Handle Game Events (Projectiles)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.StrikerHit -> {
                    // Calculate current orbital position of the specific striker
                    val i = event.strikerIndex
                    val timeMillis = System.currentTimeMillis()
                    val rotationSpeed = 3000 + i * 500
                    val currentRotation = ( (timeMillis % rotationSpeed).toFloat() / rotationSpeed) * 360f
                    
                    val r = halfBoxSizePx * 0.9f + (i * 5f)
                    val startX = halfBoxSizePx + cos(Math.toRadians(currentRotation.toDouble())).toFloat() * r
                    val startY = halfBoxSizePx + sin(Math.toRadians(currentRotation.toDouble())).toFloat() * r

                    projectiles.add(StrikerProjectile(System.nanoTime(), Offset(startX, startY), Offset(halfBoxSizePx, halfBoxSizePx), event.damage))
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

    if (!state.isTutorialCompleted) {
        AlertDialog(
            onDismissRequest = { viewModel.completeTutorial() },
            title = { Text("Welcome to Titan's Heart") },
            text = { Text("Tap the Heart to knock loose Crystal Shards. Sweep them to bank them, or recruit Sprites to automate the work!") },
            confirmButton = {
                Button(onClick = { viewModel.completeTutorial() }) { Text("Got it!") }
            }
        )
    }

    Scaffold(
        containerColor = VoidIndigo,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MoonMist),
                title = { Text("Titan's Heart", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (state.starlight > 0 || state.permanentTalents.isNotEmpty()) {
                        IconButton(onClick = onNavigateToConstellation) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = EmberGold)
                        }
                    }
                },
                actions = {
                    ArcanePanel(modifier = Modifier.padding(end = 16.dp)) {
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Text(NumberFormatter.format(state.shardsBanked), color = EmberGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("Shards", fontSize = 10.sp, color = MoonMist.copy(alpha = 0.7f))
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

                Box(
                    modifier = Modifier.size(280.dp)
                        .graphicsLayer(scaleX = idleScale * tapAnimatable.value, scaleY = idleScale * tapAnimatable.value, translationX = shakeOffset.value.x, translationY = shakeOffset.value.y)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                viewModel.onTitanTap()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                damageNumbers.add(DamageNumber(System.nanoTime(), state.clickDamage, offset))
                                repeat(3) { particles.add(ShardParticle(System.nanoTime() + it, offset)) }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    TitanCanvas(hpFraction = (state.titanHp / state.maxTitanHp).toFloat(), flashAlpha = flashAlpha.value, isBrittle = state.frostSprites > 0, isBurning = state.emberSprites > 0)
                    SpriteMotes(state, boxSizePx)
                    projectiles.forEach { proj -> key(proj.id) { StrikerProjectileMote(proj, onHit = { projectiles.remove(proj); damageNumbers.add(DamageNumber(System.nanoTime(), proj.damage, proj.target, isCrit = false)); repeat(2) { particles.add(ShardParticle(System.nanoTime() + it, proj.target)) } }) } }
                    state.activeCracks.forEach { crack -> Weakspot(crack = crack, boxSizePx = boxSizePx, onTap = { viewModel.onCrackTap(crack.id); haptic.performHapticFeedback(HapticFeedbackType.LongPress); val crackPos = Offset(crack.x * boxSizePx, crack.y * boxSizePx); damageNumbers.add(DamageNumber(System.nanoTime(), state.clickDamage * 5, crackPos, isCrit = true)); repeat(8) { particles.add(ShardParticle(System.nanoTime() + it, crackPos)) } }) }
                    damageNumbers.forEach { dn -> key(dn.id) { FloatingDamageText(dn, boxSizePx) { damageNumbers.remove(dn) } } }
                    particles.forEach { p -> key(p.id) { ShardParticleMote(p, boxSizePx) { particles.remove(p) } } }
                    ShardPile(state.shardsOnGround, boxSizePx)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val hpProgress = (state.titanHp / state.maxTitanHp).toFloat()
                    val animatedHp by animateFloatAsState(targetValue = hpProgress, label = "hpProgress")
                    Box(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                        LinearProgressIndicator(progress = { animatedHp }, modifier = Modifier.fillMaxSize().clip(CircleShape), color = if (state.emberSprites > 0) CrackMagma else SpectralCyan, trackColor = MysticBlue.copy(alpha = 0.3f)
                        )
                    }
                    Text("${NumberFormatter.format(state.titanHp)} / ${NumberFormatter.format(state.maxTitanHp)}", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelSmall, color = MoonMist)
                }

                Spacer(modifier = Modifier.weight(0.1f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArcaneButton(onClick = onNavigateToUpgrades, modifier = Modifier.weight(1f), containerColor = MysticBlue) { Text("Upgrades", style = MaterialTheme.typography.labelSmall) }
                    if (state.awakeningStage >= 1 || state.starlight > 0) {
                        ArcaneButton(onClick = { viewModel.onRebirth() }, modifier = Modifier.weight(1f), containerColor = ArcanePurple) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Rebirth", style = MaterialTheme.typography.labelSmall)
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
fun TitanCanvas(hpFraction: Float, flashAlpha: Float, isBrittle: Boolean, isBurning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowIntensity by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 0.8f, animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "glowIntensity")
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
        drawPath(path = path, brush = Brush.radialGradient(colors = listOf(MysticBlue, VoidIndigo), center = center, radius = radius * 1.5f))
        val veinColor = if (isBurning) CrackMagma else SpectralCyan
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
        drawPath(path = path, color = if (isBrittle) Color(0xFFADD8E6) else MysticBlue, style = Stroke(width = 2.dp.toPx()))
        if (flashAlpha > 0) drawPath(path = path, color = Color.White.copy(alpha = flashAlpha))
    }
}

@Composable
fun ShardPile(shards: Double, boxSizePx: Float) {
    if (shards <= 0) return
    val count = min(shards.toInt(), 30)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val random = java.util.Random(123)
        val center = Offset(size.width / 2, size.height * 0.85f)
        repeat(count) {
            val offset = Offset(random.nextFloat() * 100f - 50f, random.nextFloat() * 40f - 20f)
            drawCircle(color = EmberGold, radius = 3f, center = center + offset)
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

@Composable
fun SpriteMotes(state: com.centelles.titan.logic.GameState, boxSizePx: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "sprites")
    val halfSize = boxSizePx / 2
    repeat(min(state.strikersCount, 10)) { i ->
        val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(3000 + i * 500, easing = LinearEasing)), label = "")
        Mote(rotation, halfSize * 0.9f + (i * 5f), SpectralCyan)
    }
    repeat(min(state.gatherersCount, 8)) { i ->
        val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(2500 + i * 400, easing = LinearEasing)), label = "")
        Mote(rotation, 40f + (i * 5f), Color(0xFF4CAF50), centerY = boxSizePx * 0.35f)
    }
    repeat(min(state.thornSprites, 5)) { i ->
        Mote(i * 72f, 60f, Color(0xFFFFC107), centerY = boxSizePx * 0.35f, sizeDp = 4.dp)
    }
}

@Composable
fun Mote(angle: Float, radius: Float, color: Color, centerX: Float = 0f, centerY: Float = 0f, sizeDp: androidx.compose.ui.unit.Dp = 6.dp) {
    val x = cos(Math.toRadians(angle.toDouble())).toFloat() * radius + centerX
    val y = sin(Math.toRadians(angle.toDouble())).toFloat() * radius + centerY
    Canvas(modifier = Modifier.size(sizeDp).offset { IntOffset(x.toInt(), y.toInt()) }) {
        drawCircle(brush = Brush.radialGradient(colors = listOf(color, Color.Transparent), center = center, radius = size.width / 2))
        drawCircle(color = color, radius = 2.dp.toPx(), center = center)
    }
}

data class DamageNumber(val id: Long, val amount: Double, val pos: Offset, val isCrit: Boolean = false)
data class ShardParticle(val id: Long, val startPos: Offset)
data class StrikerProjectile(val id: Long, val start: Offset, val target: Offset, val damage: Double)

@Composable
fun StrikerProjectileMote(proj: StrikerProjectile, onHit: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(400, easing = FastOutLinearInEasing))
        onHit()
    }
    val currentPos = Offset(proj.start.x + (proj.target.x - proj.start.x) * progress.value, proj.start.y + (proj.target.y - proj.start.y) * progress.value)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(color = SpectralCyan, radius = 4f, center = currentPos)
        drawCircle(color = SpectralCyan.copy(alpha = 0.3f), radius = 10f, center = currentPos)
    }
}

@Composable
fun FloatingDamageText(dn: DamageNumber, boxSizePx: Float, onAnimationEnd: () -> Unit) {
    val animatable = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        animatable.animateTo(1f, animationSpec = tween(800, easing = LinearOutSlowInEasing))
        onAnimationEnd()
    }
    Text(text = NumberFormatter.format(dn.amount), color = if (dn.isCrit) CrackMagma else MoonMist, fontWeight = if (dn.isCrit) FontWeight.ExtraBold else FontWeight.Bold, fontSize = if (dn.isCrit) 20.sp else 16.sp, modifier = Modifier.offset { IntOffset((dn.pos.x - boxSizePx/2).toInt() - 25, (dn.pos.y - boxSizePx/2).toInt() - 50 - (animatable.value * 150).toInt()) }.graphicsLayer(alpha = 1f - animatable.value).zIndex(10f))
}

@Composable
fun ShardParticleMote(p: ShardParticle, boxSizePx: Float, onAnimationEnd: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val randomX = remember { KotlinRandom.nextFloat() * 160 - 80 }
    val randomY = remember { KotlinRandom.nextFloat() * 100 }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        onAnimationEnd()
    }
    Canvas(modifier = Modifier.size(8.dp).offset {
        val targetXPx = p.startPos.x + (randomX * progress.value)
        val targetYPx = p.startPos.y + (randomY * progress.value + (progress.value * progress.value * 200))
        IntOffset((targetXPx - boxSizePx/2).toInt() - 4.dp.toPx().toInt(), (targetYPx - boxSizePx/2).toInt() - 4.dp.toPx().toInt())
    }.graphicsLayer(alpha = 1f - progress.value, scaleX = 1f - progress.value, scaleY = 1f - progress.value)) { drawCircle(color = EmberGold) }
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
