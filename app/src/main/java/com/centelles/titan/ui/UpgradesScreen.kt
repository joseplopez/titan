package com.centelles.titan.ui

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centelles.titan.R
import com.centelles.titan.logic.GameState
import com.centelles.titan.logic.GameViewModel
import com.centelles.titan.ui.components.AdButton
import com.centelles.titan.ui.components.ArcaneButton
import com.centelles.titan.ui.components.ArcanePanel
import com.centelles.titan.ui.theme.*
import com.centelles.titan.util.NumberFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradesScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    Scaffold(
        containerColor = VoidIndigo,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MoonMist
                ),
                title = { Text(stringResource(R.string.upgrades_and_store), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = MoonMist)
                    }
                },
                actions = {
                    ArcanePanel(modifier = Modifier.padding(end = 16.dp)) {
                        Text(
                            NumberFormatter.format(state.shardsBanked),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = EmberGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(stringResource(R.string.store), EmberGold)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rewardAmount = (state.totalCps * 100.0).coerceAtLeast(10.0)
                    AdButton(
                        label = "+${NumberFormatter.format(rewardAmount)} Shards",
                        cooldownKey = GameState.AD_SHARDS,
                        state = state,
                        modifier = Modifier.weight(1f),
                        onClick = { activity?.let { viewModel.watchAdForShards(it) } }
                    )
                    
                    AdButton(
                        label = "2x Boost (3m)",
                        cooldownKey = GameState.AD_MULTIPLIER,
                        state = state,
                        modifier = Modifier.weight(1f),
                        onClick = { activity?.let { viewModel.watchAdForBoost(it) } }
                    )
                }
            }
            
            item {
                SectionHeader(stringResource(R.string.buildings), SpectralCyan)
            }
            
            item {
                UpgradeItem(
                    name = stringResource(R.string.grove),
                    description = stringResource(R.string.grove_desc),
                    level = state.grovesCount,
                    costString = NumberFormatter.format(state.getGroveCost()),
                    canAfford = state.shardsBanked >= state.getGroveCost(),
                    onBuy = { viewModel.buildGrove() },
                    glyph = { BuildingGlyph() },
                    upgradeId = "grove",
                    state = state,
                    viewModel = viewModel
                )
            }

            item {
                SectionHeader(stringResource(R.string.titan_upgrades), SpectralCyan)
            }

            item {
                UpgradeItem(
                    name = stringResource(R.string.sharpened_focus),
                    description = stringResource(R.string.sharpened_focus_desc),
                    level = state.upgrades.getOrDefault("click_power", 0),
                    costString = NumberFormatter.format(state.getUpgradeCost("click_power")),
                    canAfford = state.shardsBanked >= state.getUpgradeCost("click_power"),
                    onBuy = { viewModel.buyUpgrade("click_power") },
                    glyph = { TitanGlyph() },
                    upgradeId = "click_power",
                    state = state,
                    viewModel = viewModel
                )
            }

            item {
                SectionHeader(stringResource(R.string.enchanted_garden), SpectralCyan)
            }

            item {
                UpgradeItem(
                    name = stringResource(R.string.unlock_thorn_sprites),
                    description = stringResource(R.string.unlock_thorn_sprites_desc),
                    level = if (state.isUpgradeUnlocked("unlock_thorn")) 1 else 0,
                    costString = NumberFormatter.format(state.getUpgradeCost("unlock_thorn")),
                    canAfford = state.shardsBanked >= state.getUpgradeCost("unlock_thorn"),
                    enabled = !state.isUpgradeUnlocked("unlock_thorn"),
                    onBuy = { viewModel.buyUpgrade("unlock_thorn") },
                    glyph = { GardenGlyph() },
                    upgradeId = "unlock_thorn",
                    state = state,
                    viewModel = viewModel
                )
            }

            item {
                UpgradeItem(
                    name = stringResource(R.string.unlock_ember_sprites),
                    description = stringResource(R.string.unlock_ember_sprites_desc),
                    level = if (state.isUpgradeUnlocked("unlock_ember")) 1 else 0,
                    costString = NumberFormatter.format(state.getUpgradeCost("unlock_ember")),
                    canAfford = state.shardsBanked >= state.getUpgradeCost("unlock_ember"),
                    enabled = !state.isUpgradeUnlocked("unlock_ember"),
                    onBuy = { viewModel.buyUpgrade("unlock_ember") },
                    glyph = { GardenGlyph() },
                    upgradeId = "unlock_ember",
                    state = state,
                    viewModel = viewModel
                )
            }

            item {
                UpgradeItem(
                    name = stringResource(R.string.unlock_frost_sprites),
                    description = stringResource(R.string.unlock_frost_sprites_desc),
                    level = if (state.isUpgradeUnlocked("unlock_frost")) 1 else 0,
                    costString = NumberFormatter.format(state.getUpgradeCost("unlock_frost")),
                    canAfford = state.shardsBanked >= state.getUpgradeCost("unlock_frost"),
                    enabled = !state.isUpgradeUnlocked("unlock_frost"),
                    onBuy = { viewModel.buyUpgrade("unlock_frost") },
                    glyph = { GardenGlyph() },
                    upgradeId = "unlock_frost",
                    state = state,
                    viewModel = viewModel
                )
            }

            item {
                SectionHeader(stringResource(R.string.sprite_upgrades), SpectralCyan)
            }

            item {
                UpgradeItem(
                    name = stringResource(R.string.sprite_academy),
                    description = stringResource(R.string.sprite_academy_desc),
                    level = state.upgrades.getOrDefault("sprite_efficiency", 0),
                    costString = NumberFormatter.format(state.getUpgradeCost("sprite_efficiency")),
                    canAfford = state.shardsBanked >= state.getUpgradeCost("sprite_efficiency"),
                    onBuy = { viewModel.buyUpgrade("sprite_efficiency") },
                    upgradeId = "sprite_efficiency",
                    state = state,
                    viewModel = viewModel
                )
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String, color: Color) {
    Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = color)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = color.copy(alpha = 0.3f))
    }
}

@Composable
fun UpgradeItem(
    name: String,
    description: String,
    level: Int,
    costString: String,
    canAfford: Boolean,
    enabled: Boolean = true,
    onBuy: () -> Unit,
    glyph: @Composable (() -> Unit)? = null,
    upgradeId: String? = null,
    state: GameState? = null,
    viewModel: GameViewModel? = null
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = context as? Activity
    
    val infiniteTransition = rememberInfiniteTransition(label = "buyPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    ArcanePanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (glyph != null) {
                Box(modifier = Modifier.size(40.dp).padding(end = 12.dp)) {
                    glyph()
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MoonMist)
                Text(description, fontSize = 12.sp, color = MoonMist.copy(alpha = 0.7f))
                Text(stringResource(R.string.level_label, level), fontSize = 12.sp, color = SpectralCyan)
            }

            if (upgradeId != null && state != null && viewModel != null && enabled) {
                AdButton(
                    label = "Free",
                    cooldownKey = GameState.AD_FREE_UPGRADE,
                    state = state,
                    onClick = { activity?.let { viewModel.watchAdForFreeUpgrade(it, upgradeId) } },
                    compact = true
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            ArcaneButton(
                onClick = {
                    onBuy()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                enabled = canAfford && enabled,
                containerColor = if (canAfford) EmberGold.copy(alpha = pulseAlpha) else MysticBlue,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (enabled) {
                    Text(
                        costString, 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (canAfford) VoidIndigo else MoonMist.copy(alpha = 0.7f)
                    )
                } else {
                    Text(stringResource(R.string.maxed), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun BuildingGlyph() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = SpectralCyan, style = Stroke(width = 2.dp.toPx()))
        drawLine(SpectralCyan, center.copy(y = 0f), center.copy(y = size.height), strokeWidth = 1.dp.toPx())
    }
}

@Composable
fun TitanGlyph() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(color = SpectralCyan, style = Stroke(width = 2.dp.toPx()))
        drawCircle(color = SpectralCyan, radius = size.width / 4)
    }
}

@Composable
fun GardenGlyph() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawPath(
            Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            },
            color = SpectralCyan,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
