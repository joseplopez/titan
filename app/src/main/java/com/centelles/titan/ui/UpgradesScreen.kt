package com.centelles.titan.ui

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centelles.titan.logic.GameViewModel
import com.centelles.titan.ui.components.ArcaneButton
import com.centelles.titan.ui.components.ArcanePanel
import com.centelles.titan.ui.theme.*
import com.centelles.titan.util.NumberFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradesScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = VoidIndigo,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MoonMist
                ),
                title = { Text("Upgrades & Store", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MoonMist)
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
                SectionHeader("Store", EmberGold)
            }
            
            item {
                UpgradeItem(
                    name = "Remove Ads",
                    description = "Permanent 2X Boost button.",
                    level = if (state.adsRemoved) 1 else 0,
                    costString = "$2.99",
                    canAfford = true,
                    enabled = !state.adsRemoved,
                    onBuy = { viewModel.purchaseRemoveAds() }
                )
            }

            item {
                SectionHeader("Buildings", SpectralCyan)
            }
            
            item {
                UpgradeItem(
                    name = "Grove",
                    description = "Increases Sprite cap by 10.",
                    level = state.grovesCount,
                    costString = NumberFormatter.format(state.getGroveCost()),
                    canAfford = state.shardsBanked >= state.getGroveCost(),
                    onBuy = { viewModel.buildGrove() },
                    glyph = { BuildingGlyph() }
                )
            }

            item {
                SectionHeader("Titan Upgrades", SpectralCyan)
            }

            item {
                UpgradeItem(
                    name = "Sharpened Focus",
                    description = "+10% click damage.",
                    level = state.upgrades.getOrDefault("click_power", 0),
                    costString = NumberFormatter.format(state.getUpgradeCost("click_power")),
                    canAfford = state.shardsBanked >= state.getUpgradeCost("click_power"),
                    onBuy = { viewModel.buyUpgrade("click_power") },
                    glyph = { TitanGlyph() }
                )
            }

            item {
                SectionHeader("Enchanted Garden", SpectralCyan)
            }

            item {
                UpgradeItem(
                    name = "Unlock Thorn Sprites",
                    description = "Master collectors.",
                    level = if (state.isUpgradeUnlocked("unlock_thorn")) 1 else 0,
                    costString = NumberFormatter.format(state.getUpgradeCost("unlock_thorn")),
                    canAfford = state.shardsBanked >= state.getUpgradeCost("unlock_thorn"),
                    enabled = !state.isUpgradeUnlocked("unlock_thorn"),
                    onBuy = { viewModel.buyUpgrade("unlock_thorn") },
                    glyph = { GardenGlyph() }
                )
            }

            item {
                UpgradeItem(
                    name = "Unlock Ember Sprites",
                    description = "Fiery DOT damage.",
                    level = if (state.isUpgradeUnlocked("unlock_ember")) 1 else 0,
                    costString = NumberFormatter.format(state.getUpgradeCost("unlock_ember")),
                    canAfford = state.shardsBanked >= state.getUpgradeCost("unlock_ember"),
                    enabled = !state.isUpgradeUnlocked("unlock_ember"),
                    onBuy = { viewModel.buyUpgrade("unlock_ember") },
                    glyph = { GardenGlyph() }
                )
            }

            item {
                UpgradeItem(
                    name = "Unlock Frost Sprites",
                    description = "Damage amplifier.",
                    level = if (state.isUpgradeUnlocked("unlock_frost")) 1 else 0,
                    costString = NumberFormatter.format(state.getUpgradeCost("unlock_frost")),
                    canAfford = state.shardsBanked >= state.getUpgradeCost("unlock_frost"),
                    enabled = !state.isUpgradeUnlocked("unlock_frost"),
                    onBuy = { viewModel.buyUpgrade("unlock_frost") },
                    glyph = { GardenGlyph() }
                )
            }

            item {
                SectionHeader("Sprite Upgrades", SpectralCyan)
            }

            item {
                UpgradeItem(
                    name = "Sprite Academy",
                    description = "+10% Sprite efficiency.",
                    level = state.upgrades.getOrDefault("sprite_efficiency", 0),
                    costString = NumberFormatter.format(state.getUpgradeCost("sprite_efficiency")),
                    canAfford = state.shardsBanked >= state.getUpgradeCost("sprite_efficiency"),
                    onBuy = { viewModel.buyUpgrade("sprite_efficiency") }
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
    glyph: @Composable (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
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
                Text("Level: $level", fontSize = 12.sp, color = SpectralCyan)
            }
            ArcaneButton(
                onClick = {
                    onBuy()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                enabled = canAfford && enabled,
                containerColor = if (canAfford) EmberGold else MysticBlue,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                if (enabled) {
                    Text(costString, style = MaterialTheme.typography.labelSmall)
                } else {
                    Text("Maxed", style = MaterialTheme.typography.labelSmall)
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
