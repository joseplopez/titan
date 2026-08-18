package com.centelles.titan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.centelles.titan.logic.GameViewModel
import com.centelles.titan.ui.TitanScreen
import com.centelles.titan.ui.UpgradesScreen
import com.centelles.titan.ui.ConstellationScreen
import com.centelles.titan.ui.StoryIntroScreen
import com.centelles.titan.ui.theme.TitanTheme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.centelles.titan.logic.GameEvent
import com.centelles.titan.ui.LayerIntroScreen
import com.centelles.titan.util.TitanAnalytics
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TitanAnalytics.initialize(this)
        enableEdgeToEdge()
        setContent {
            TitanTheme {
                val navController = rememberNavController()
                val state by viewModel.state.collectAsState()
                
                var showLayerIntro by remember { mutableStateOf<Int?>(null) }

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        if (event is GameEvent.LayerStoryReveal) {
                            showLayerIntro = event.layer
                        }
                    }
                }

                val startDestination = if (state.hasSeenIntro) "titan" else "story_intro"

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("story_intro") {
                            StoryIntroScreen(
                                onFinished = {
                                    viewModel.setHasSeenIntro()
                                    navController.navigate("titan") {
                                        popUpTo("story_intro") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("titan") {
                            TitanScreen(
                                viewModel = viewModel,
                                onNavigateToUpgrades = { navController.navigate("upgrades") },
                                onNavigateToConstellation = { navController.navigate("constellation") }
                            )
                        }
                        composable("upgrades") {
                            UpgradesScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("constellation") {
                            ConstellationScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Layer Intro Overlay
                    showLayerIntro?.let { layer ->
                        LayerIntroScreen(
                            layer = layer,
                            onFinished = {
                                viewModel.markLayerIntroSeen(layer)
                                showLayerIntro = null
                            }
                        )
                    }
                }
            }
        }
    }
}
