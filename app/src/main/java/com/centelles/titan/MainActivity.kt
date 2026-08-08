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

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TitanTheme {
                val navController = rememberNavController()
                val state by viewModel.state.collectAsState()
                
                val startDestination = if (state.hasSeenIntro) "titan" else "story_intro"

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
            }
        }
    }
}
