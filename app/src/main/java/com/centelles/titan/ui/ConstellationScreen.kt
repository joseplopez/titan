package com.centelles.titan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centelles.titan.logic.GameViewModel
import com.centelles.titan.ui.components.ArcanePanel
import com.centelles.titan.ui.theme.EmberGold
import com.centelles.titan.ui.theme.MoonMist
import com.centelles.titan.ui.theme.SpectralCyan
import com.centelles.titan.ui.theme.VoidIndigo
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstellationScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = VoidIndigo,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MoonMist),
                title = { Text("Constellation") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = MoonMist) }
                },
                actions = {
                    Text(
                        "Starlight: ${String.format(Locale.US, "%.1f", state.starlight)} ⭐",
                        modifier = Modifier.padding(end = 16.dp),
                        color = SpectralCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = SpectralCyan,
                divider = {}
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Might", modifier = Modifier.padding(16.dp))
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { if (state.deepestLayerReached >= 2) selectedTab = 1 },
                    enabled = state.deepestLayerReached >= 2
                ) {
                    Text(if (state.deepestLayerReached >= 2) "Craft" else "🔒 Craft", modifier = Modifier.padding(16.dp))
                }
                Tab(
                    selected = selectedTab == 2,
                    onClick = { if (state.deepestLayerReached >= 3) selectedTab = 2 },
                    enabled = state.deepestLayerReached >= 3
                ) {
                    Text(if (state.deepestLayerReached >= 3) "Wild" else "🔒 Wild", modifier = Modifier.padding(16.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        item { TalentNode("starlight_dps", "Celestial Might", "+20% DPS", state, viewModel) }
                        item { TalentNode("starlight_crit", "Stellar Focus", "+10% Crack Damage", state, viewModel) }
                    }
                    1 -> {
                        item { TalentNode("starlight_cps", "Astral Greed", "+20% CPS", state, viewModel) }
                        item { TalentNode("starlight_costs", "Spirit Call", "-5% Sprite Costs", state, viewModel) }
                    }
                    2 -> {
                        item { TalentNode("starlight_capacity", "Expansive Groves", "+5 Sprite Capacity", state, viewModel) }
                        item { TalentNode("starlight_elements", "Primordial Aura", "+15% Elemental Potency", state, viewModel) }
                    }
                }
            }
        }
    }
}

@Composable
fun TalentNode(id: String, name: String, description: String, state: com.centelles.titan.logic.GameState, viewModel: GameViewModel) {
    val level = state.permanentTalents.getOrDefault(id, 0)
    val cost = state.getTalentCost(id)
    val canAfford = state.starlight >= cost

    ArcanePanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                Text(description, fontSize = 12.sp, color = MoonMist)
                Text("Rank: $level", fontSize = 14.sp, color = SpectralCyan)
            }
            Button(
                onClick = { viewModel.buyTalent(id) },
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(containerColor = SpectralCyan, contentColor = VoidIndigo)
            ) {
                Text("${String.format(Locale.US, "%.1f", cost)} ⭐")
            }
        }
    }
}
