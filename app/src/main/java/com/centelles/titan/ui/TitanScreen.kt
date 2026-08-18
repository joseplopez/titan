package com.centelles.titan.ui

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.centelles.titan.R
import com.centelles.titan.logic.GameState
import com.centelles.titan.logic.GameEvent
import com.centelles.titan.logic.GameViewModel
import com.centelles.titan.ui.components.ArcaneButton
import com.centelles.titan.ui.components.ArcanePanel
import com.centelles.titan.ui.theme.*
import com.centelles.titan.util.NumberFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val activity = context as? Activity
    val density = LocalDensity.current
    
    var showDescendDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val boxSizePx = with(density) { 240.dp.toPx() }
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
    val landedShards = remember { mutableStateListOf<Offset>() }

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
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.checkAndTriggerLayerIntro()
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

    /* Removed redundant HP sync animation to avoid conflicts with manual tap animation */
    /* LaunchedEffect(state.titanHp) {
        tapAnimatable.animateTo(0.95f, animationSpec = tween(50))
        tapAnimatable.animateTo(1f, animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f))
    } */

    LaunchedEffect(state.currentRunShardsEarned) {
        flashAlpha.animateTo(0.4f, animationSpec = tween(50))
        flashAlpha.animateTo(0f, animationSpec = tween(200))
        shakeOffset.animateTo(Offset(KotlinRandom.nextFloat() * 10 - 5, KotlinRandom.nextFloat() * 10 - 5), animationSpec = tween(50))
        shakeOffset.animateTo(Offset.Zero, animationSpec = spring(stiffness = Spring.StiffnessHigh))
    }

    // Sync Landed Shards with State
    LaunchedEffect(state.shardsOnGround) {
        val targetCount = min(state.shardsOnGround.toInt(), 500)
        if (landedShards.size < targetCount) {
            repeat(targetCount - landedShards.size) {
                landedShards.add(Offset(KotlinRandom.nextFloat() * groundAreaWidthPx - groundAreaWidthPx / 2, KotlinRandom.nextFloat() * with(density) { 80.dp.toPx() }))
            }
        } else if (landedShards.size > targetCount + 50) {
            repeat(landedShards.size - targetCount) { landedShards.removeAt(0) }
        }
    }

    // Thorn and Gatherer Collision Detection (Cleaning up landed shards)
    LaunchedEffect(animTime) {
        if ((state.thornSprites > 0 || state.gatherersCount > 0) && landedShards.isNotEmpty()) {
            val visibleThorns = state.thornSprites
            val visibleGatherers = state.gatherersCount
            val groundAreaHeight = with(density) { 60.dp.toPx() } // Match UI height
            
            val spritePositions = mutableListOf<Offset>()
            
            // Centralized sprite position logic (exactly matches drawing logic)
            repeat(visibleThorns) { i ->
                val patrolWidth = groundAreaWidthPx * 0.9f
                val hSpeed = 0.0006f + (i * 0.0001f)
                val vSpeed = 0.0004f + (i * 0.00008f)
                
                val x = sin(animTime * hSpeed + i).toFloat() * patrolWidth / 2
                // Expanded vertical range to cover top to bottom (0.1 to 0.9 of height)
                val y = groundAreaHeight * (0.5f + sin(animTime * vSpeed + i * 1.5f).toFloat() * 0.4f)
                spritePositions.add(Offset(x, y))
            }
            
            repeat(visibleGatherers) { i ->
                val patrolWidth = groundAreaWidthPx * 0.95f
                val hSpeed = 0.0004f + (i * 0.00007f)
                val vSpeed = 0.00025f + (i * 0.00005f)
                
                val x = sin(animTime * hSpeed + i * 2.1f).toFloat() * patrolWidth / 2
                // Expanded vertical range to cover top to bottom (0.2 to 0.8 of height)
                val y = groundAreaHeight * (0.5f + sin(animTime * vSpeed + i * 0.7f).toFloat() * 0.3f)
                spritePositions.add(Offset(x, y))
            }

            val toRemove = mutableListOf<Offset>()
            landedShards.forEach { shard ->
                // Both shard and sprite are now in "center-relative" ground coordinates
                if (spritePositions.any { sprite -> (shard - sprite).getDistance() < 30f }) {
                    toRemove.add(shard)
                }
            }
            
            if (toRemove.isNotEmpty()) {
                landedShards.removeAll(toRemove)
            }
        }
    }

    // Thorn Collision Detection (FALLING PARTICLES - keeping this for cleanup)
    LaunchedEffect(animTime) {
        if (state.thornSprites > 0 && particles.isNotEmpty()) {
            val visibleThorns = min(state.thornSprites, 5)
            val groundAreaTopFromCenter = with(density) { 156.dp.toPx() }
            val groundAreaHeight = with(density) { 80.dp.toPx() }
            val fallDistance = groundAreaTopFromCenter + groundAreaHeight / 2
            
            val thorns = (0 until visibleThorns).map { i ->
                val patrolWidth = groundAreaWidthPx * 0.85f
                val hSpeed1 = 0.0006f + (i * 0.00005f)
                val hSpeed2 = 0.00035f + (i * 0.000025f)
                val vSpeed1 = 0.00045f + (i * 0.00004f)
                val vSpeed2 = 0.00025f + (i * 0.00002f)
                
                val x = (sin(animTime * hSpeed1 + i * 2.1f) * 0.7f + sin(animTime * hSpeed2 + i * 0.7f) * 0.3f) * patrolWidth / 2
                val yInGround = groundAreaHeight * (0.5f + (sin(animTime * vSpeed1 + i * 1.3f) * 0.7f + sin(animTime * vSpeed2 + i * 0.4f) * 0.3f) * 0.4f)
                Offset(x.toFloat(), groundAreaTopFromCenter + yInGround.toFloat())
            }

            val toRemove = mutableListOf<ShardParticle>()
            val particleList = particles.toList()
            particleList.forEach { p ->
                val t = animTime - p.startTime
                val progress = (t.toFloat() / 800f).coerceIn(0f, 1f)
                val currentX = p.centerRelativePos.x + (p.randomX * progress)
                val currentY = p.centerRelativePos.y + (progress * fallDistance)
                val pPos = Offset(currentX, currentY)
                
                if (thorns.any { tPos -> (pPos - tPos).getDistance() < 300f }) {
                    toRemove.add(p)
                }
            }
            
            if (toRemove.isNotEmpty()) {
                Log.d("TitanCollision", "Removing ${toRemove.size} particles from ${particles.size}. FirstPartY: ${particleList.firstOrNull()?.centerRelativePos?.y}")
                particles.removeAll(toRemove)
            }
        }
    }

    Scaffold(
        containerColor = VoidIndigo,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MoonMist),
                title = { Text(stringResource(R.string.titans_heart), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToConstellation) {
                        Icon(Icons.Default.Star, contentDescription = stringResource(R.string.stars), tint = EmberGold)
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                        ArcanePanel(modifier = Modifier.clickable { onNavigateToConstellation() }) {
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("${String.format(Locale.US, "%.1f", state.starlight)}⭐", color = SpectralCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(stringResource(R.string.stars), fontSize = 8.sp, color = MoonMist.copy(alpha = 0.7f))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ArcanePanel {
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(NumberFormatter.format(state.shardsBanked), color = EmberGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.shards), fontSize = 10.sp, color = MoonMist.copy(alpha = 0.7f))
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
                    StatItem(stringResource(R.string.dps), NumberFormatter.format(state.totalDps))
                    StatItem(stringResource(R.string.cps), NumberFormatter.format(state.totalCps))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(240.dp)
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
                                        
                                        coroutineScope.launch {
                                            tapAnimatable.animateTo(0.92f, animationSpec = tween(50))
                                            tapAnimatable.animateTo(1f, animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f))
                                        }

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
                            
                            // Elemental Pulses (Fire & Ice)
                            ElementalPulses(state, animTime)
                            
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
                        
                        GroundArea(state, animTime, landedShards, modifier = Modifier.fillMaxWidth().height(60.dp))
                    }
                    
                    // Feedback Layer (Unclipped)
                    Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                        damageNumbers.forEach { dn -> key(dn.id) { FloatingDamageText(dn) { damageNumbers.remove(dn) } } }
                        particles.forEach { p -> key(p.id) { ShardParticleMote(p) { particles.remove(p) } } }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.stage_label, state.awakeningStage + 1), style = MaterialTheme.typography.labelLarge, color = EmberGold, modifier = Modifier.padding(bottom = 8.dp))

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
                    ArcaneButton(onClick = onNavigateToUpgrades, modifier = Modifier.weight(1f), containerColor = MysticBlue) { Text(stringResource(R.string.upgrades), style = MaterialTheme.typography.labelSmall) }
                    
                    val canDescend = state.canDescend()
                    ArcaneButton(
                        onClick = { if (canDescend) showDescendDialog = true },
                        modifier = Modifier.weight(1.2f),
                        containerColor = if (canDescend) ArcanePurple else Color.Gray.copy(alpha = 0.5f),
                        enabled = true
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (canDescend) stringResource(R.string.the_descent) else stringResource(R.string.reach_stage, state.currentLayerDef.finalStage),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                            if (canDescend) {
                                Text("+${String.format(Locale.US, "%.1f", state.calculateStarlightReward())}⭐", fontSize = 10.sp)
                            }
                        }
                    }

                    ArcaneButton(onClick = { viewModel.onManualCollect(); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, modifier = Modifier.weight(1f), containerColor = EmberGold) { Text(stringResource(R.string.sweep), style = MaterialTheme.typography.labelSmall) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ArcanePanel(modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.recruit_sprites_label, state.currentSpriteCount, state.spriteCapacity), style = MaterialTheme.typography.labelSmall, color = MoonMist)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RecruitButton(stringResource(R.string.striker), state.strikersCount, state.getStrikerCost(), state.shardsBanked >= state.getStrikerCost() && state.currentSpriteCount < state.spriteCapacity, SpectralCyan) { viewModel.recruitStriker() }
                                RecruitButton(stringResource(R.string.gatherer), state.gatherersCount, state.getGathererCost(), state.shardsBanked >= state.getGathererCost() && state.currentSpriteCount < state.spriteCapacity, Color(0xFF4CAF50)) { viewModel.recruitGatherer() }
                            }
                            if (state.isUpgradeUnlocked("unlock_ember") || state.isUpgradeUnlocked("unlock_frost") || state.isUpgradeUnlocked("unlock_thorn")) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (state.isUpgradeUnlocked("unlock_ember")) RecruitButton(stringResource(R.string.ember), state.emberSprites, state.getEmberCost(), state.shardsBanked >= state.getEmberCost() && state.currentSpriteCount < state.spriteCapacity, Color(0xFFFF5722)) { viewModel.recruitEmber() }
                                    if (state.isUpgradeUnlocked("unlock_frost")) RecruitButton(stringResource(R.string.frost), state.frostSprites, state.getFrostCost(), state.shardsBanked >= state.getFrostCost() && state.currentSpriteCount < state.spriteCapacity, Color(0xFF03A9F4)) { viewModel.recruitFrost() }
                                }
                                if (state.isUpgradeUnlocked("unlock_thorn")) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        RecruitButton(stringResource(R.string.thorn), state.thornSprites, state.getThornCost(), state.shardsBanked >= state.getThornCost() && state.currentSpriteCount < state.spriteCapacity, Color.Red) { viewModel.recruitThorn() }
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showDescendDialog) {
                DescendDialog(
                    state = state,
                    activity = activity,
                    viewModel = viewModel,
                    onDismiss = { showDescendDialog = false }
                )
            }
        }
    }
}

@Composable
fun DescendDialog(
    state: GameState,
    activity: Activity?,
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoidIndigo,
        title = { Text(stringResource(R.string.the_descent), color = MoonMist) },
        text = {
            Column {
                Text(
                    stringResource(R.string.descent_warning),
                    color = MoonMist.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                ArcanePanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.starlight_reward), color = MoonMist, fontSize = 14.sp)
                        Text(
                            "+${String.format(Locale.US, "%.1f", state.calculateStarlightReward())} ⭐",
                            color = SpectralCyan,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isFirstDescendCompleted) {
                    ArcaneButton(
                        onClick = {
                            activity?.let {
                                viewModel.watchAdForStarlightBoost(it) {
                                    viewModel.onDescend(starlightBoosted = true)
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = EmberGold
                    ) {
                        Text("Watch Ad: +50% Starlight", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                ArcaneButton(
                    onClick = {
                        viewModel.onDescend()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = ArcanePurple
                ) {
                    Text(stringResource(R.string.confirm_descent), style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MoonMist)
            }
        }
    )
}

@Composable
fun ElementalPulses(state: com.centelles.titan.logic.GameState, animTime: Long) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Ember Pulses (Capped at 15 for performance)
        val visibleEmbers = min(state.emberSprites, 15)
        repeat(visibleEmbers) { i ->
            val period = 3000L
            val localTime = animTime + i * 600L
            val cycle = localTime / period
            val r = java.util.Random(cycle + i * 1234L)
            val alphaProgress = (localTime % period).toFloat() / period
            val alpha = if (alphaProgress < 0.5f) alphaProgress * 2f else 1f - (alphaProgress - 0.5f) * 2f
            
            val x = (r.nextFloat() * 180f) - 90f
            val y = (r.nextFloat() * 180f) - 90f
            val pos = center + Offset(x, y)
            val radius = 35f + r.nextFloat() * 15f
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Red.copy(alpha = alpha * 0.4f), Color.Transparent),
                    center = pos,
                    radius = radius
                ),
                center = pos,
                radius = radius
            )
        }

        // Frost Pulses (Capped at 15 for performance)
        val visibleFrosts = min(state.frostSprites, 15)
        repeat(visibleFrosts) { i ->
            val period = 4000L
            val localTime = animTime + i * 800L
            val cycle = localTime / period
            val r = java.util.Random(cycle + i * 5678L)
            val alphaProgress = (localTime % period).toFloat() / period
            val alpha = if (alphaProgress < 0.5f) alphaProgress * 2f else 1f - (alphaProgress - 0.5f) * 2f
            
            val x = (r.nextFloat() * 180f) - 90f
            val y = (r.nextFloat() * 180f) - 90f
            val pos = center + Offset(x, y)
            val radius = 40f + r.nextFloat() * 15f
            val iceColor = Color(0xFFADD8E6)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(iceColor.copy(alpha = alpha * 0.4f), Color.Transparent),
                    center = pos,
                    radius = radius
                ),
                center = pos,
                radius = radius
            )
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
fun GroundArea(state: GameState, animTime: Long, landedShards: List<Offset>, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val groundAreaWidthPx = width
            
            // Draw Shards
            landedShards.forEach { shard ->
                val drawPos = Offset(width / 2 + shard.x, shard.y)
                drawCircle(
                    color = EmberGold, 
                    radius = 2.dp.toPx(), 
                    center = drawPos,
                    alpha = 0.7f
                )
            }
            
            // Draw Gatherers (Harmonic, smooth movement)
            repeat(state.gatherersCount) { i ->
                val patrolWidth = groundAreaWidthPx * 0.95f
                val hSpeed = 0.0004f + (i * 0.00007f)
                val vSpeed = 0.00025f + (i * 0.00005f)
                
                val x = sin(animTime * hSpeed + i * 2.1f).toFloat() * patrolWidth / 2
                val y = height * (0.5f + sin(animTime * vSpeed + i * 0.7f).toFloat() * 0.3f)
                
                drawCircle(color = Color(0xFF4CAF50), radius = 3.dp.toPx(), center = Offset(width / 2 + x, y))
            }
            
            // Draw Thorns (Aggressive, distinct paths)
            repeat(state.thornSprites) { i ->
                val patrolWidth = groundAreaWidthPx * 0.9f
                val hSpeed = 0.0006f + (i * 0.0001f)
                val vSpeed = 0.0004f + (i * 0.00008f)
                
                val x = sin(animTime * hSpeed + i).toFloat() * patrolWidth / 2
                val y = height * (0.5f + sin(animTime * vSpeed + i * 1.5f).toFloat() * 0.4f)
                
                drawCircle(color = Color.Red, radius = 5.dp.toPx(), center = Offset(width / 2 + x, y))
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
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        onAnimationEnd()
    }
    // Origin is center of heart
    Canvas(modifier = Modifier.size(8.dp).offset {
        val groundAreaTopFromCenter = with(density) { (140.dp + 16.dp).toPx() }
        val groundAreaHeight = with(density) { 80.dp.toPx() }
        val fallDistance = groundAreaTopFromCenter + groundAreaHeight / 2
        
        val targetXPx = p.centerRelativePos.x + (p.randomX * progress.value)
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
