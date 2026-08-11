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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centelles.titan.R
import com.centelles.titan.logic.GameViewModel
import com.centelles.titan.logic.TalentTree
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
                title = { Text(stringResource(R.string.constellation)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back), color = MoonMist) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.descend_stars_label, String.format(Locale.US, "%.1f", state.starlight)),
                    color = SpectralCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = SpectralCyan,
                divider = {}
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text(stringResource(R.string.might), modifier = Modifier.padding(16.dp))
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { if (state.deepestLayerReached >= 2) selectedTab = 1 },
                    enabled = state.deepestLayerReached >= 2
                ) {
                    val label = stringResource(R.string.craft)
                    Text(if (state.deepestLayerReached >= 2) label else "🔒 $label", modifier = Modifier.padding(16.dp))
                }
                Tab(
                    selected = selectedTab == 2,
                    onClick = { if (state.deepestLayerReached >= 3) selectedTab = 2 },
                    enabled = state.deepestLayerReached >= 3
                ) {
                    val label = stringResource(R.string.wild)
                    Text(if (state.deepestLayerReached >= 3) label else "🔒 $label", modifier = Modifier.padding(16.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val currentTree = when (selectedTab) {
                    0 -> TalentTree.MIGHT
                    1 -> TalentTree.CRAFT
                    else -> TalentTree.WILD
                }

                com.centelles.titan.logic.GameState.TALENTS.filter { it.tree == currentTree }.forEach { talent ->
                    item { TalentNode(talent, state, viewModel) }
                }
            }
        }
    }
}

@Composable
fun TalentNode(talent: com.centelles.titan.logic.Talent, state: com.centelles.titan.logic.GameState, viewModel: GameViewModel) {
    val level = state.permanentTalents.getOrDefault(talent.id, 0)
    val cost = state.getTalentCost(talent.id)
    val unlocked = state.canUnlockTalent(talent.id)
    val canAfford = state.starlight >= cost && unlocked

    ArcanePanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(talent.nameRes), fontWeight = FontWeight.Bold, color = if (unlocked) Color.White else Color.Gray)
                Text(stringResource(talent.descriptionRes), fontSize = 12.sp, color = MoonMist)
                
                if (!unlocked) {
                    talent.prerequisites.forEach { (prereqId, reqLevel) ->
                        val prereqTalent = com.centelles.titan.logic.GameState.TALENTS.find { it.id == prereqId }
                        val prereqName = if (prereqTalent != null) stringResource(prereqTalent.nameRes) else prereqId
                        Text(stringResource(R.string.requires_rank, prereqName, reqLevel), fontSize = 10.sp, color = Color.Red.copy(alpha = 0.7f))
                    }
                } else {
                    Text(stringResource(R.string.rank_label, level), fontSize = 14.sp, color = SpectralCyan)
                }
            }
            Button(
                onClick = { viewModel.buyTalent(talent.id) },
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(containerColor = SpectralCyan, contentColor = VoidIndigo)
            ) {
                Text("${String.format(Locale.US, "%.1f", cost)} ⭐")
            }
        }
    }
}
