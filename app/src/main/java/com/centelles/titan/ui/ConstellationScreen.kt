package com.centelles.titan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centelles.titan.logic.GameViewModel
import com.centelles.titan.util.NumberFormatter

import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstellationScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Constellation Tree") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    Text(
                        "Starlight: ${String.format(Locale.US, "%.1f", state.starlight)}",
                        modifier = Modifier.padding(end = 16.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Permanent Blessings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "These talents persist across all Rebirths.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                TalentItem(
                    name = "Celestial Might",
                    description = "+20% DPS per level (Tap & Strikers).",
                    level = state.permanentTalents.getOrDefault("starlight_dps", 0),
                    cost = state.getTalentCost("starlight_dps"),
                    canAfford = state.starlight >= state.getTalentCost("starlight_dps"),
                    onBuy = { viewModel.buyTalent("starlight_dps") }
                )
            }

            item {
                TalentItem(
                    name = "Astral Greed",
                    description = "+20% CPS per level.",
                    level = state.permanentTalents.getOrDefault("starlight_cps", 0),
                    cost = state.getTalentCost("starlight_cps"),
                    canAfford = state.starlight >= state.getTalentCost("starlight_cps"),
                    onBuy = { viewModel.buyTalent("starlight_cps") }
                )
            }

            item {
                TalentItem(
                    name = "Expansive Groves",
                    description = "+5 Sprite capacity per level.",
                    level = state.permanentTalents.getOrDefault("starlight_capacity", 0),
                    cost = state.getTalentCost("starlight_capacity"),
                    canAfford = state.starlight >= state.getTalentCost("starlight_capacity"),
                    onBuy = { viewModel.buyTalent("starlight_capacity") }
                )
            }

            item {
                TalentItem(
                    name = "Spirit Call",
                    description = "-5% Sprite recruitment cost per level.",
                    level = state.permanentTalents.getOrDefault("starlight_costs", 0),
                    cost = state.getTalentCost("starlight_costs"),
                    canAfford = state.starlight >= state.getTalentCost("starlight_costs"),
                    onBuy = { viewModel.buyTalent("starlight_costs") }
                )
            }
        }
    }
}

@Composable
fun TalentItem(
    name: String,
    description: String,
    level: Int,
    cost: Double,
    canAfford: Boolean,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Rank: $level", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onBuy,
                enabled = canAfford,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("${String.format(Locale.US, "%.1f", cost)} ⭐", fontSize = 14.sp)
            }
        }
    }
}
