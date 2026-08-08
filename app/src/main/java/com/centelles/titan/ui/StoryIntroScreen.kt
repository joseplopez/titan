package com.centelles.titan.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centelles.titan.ui.components.ArcaneButton
import com.centelles.titan.ui.theme.EmberGold
import com.centelles.titan.ui.theme.MoonMist
import com.centelles.titan.ui.theme.VoidIndigo
import kotlinx.coroutines.delay

@Composable
fun StoryIntroScreen(onFinished: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val storyParagraphs = listOf(
        "Before the world had a name, a Titan walked it.",
        "It did not wage war, nor seek dominion. It simply slept — curled beneath the mountains, dreaming a dream so vast that stars formed in its wake. Over eons, its dreaming heart crystallized: a core of living crystal, humming with the memory of everything it had ever seen.",
        "The mountains wore away. Kingdoms rose atop its shoulders without ever knowing what slept beneath. And in time, the last of the old fey folk — small, curious, and utterly fearless — found the crack in the stone that led to its heart.",
        "They call themselves Sprites now. They do not know if waking the Titan will end the world, or begin a new one.",
        "They intend to find out."
    )

    LaunchedEffect(Unit) {
        delay(1000)
        step = 1 // Title materialize
        delay(2000)
        step = 2 // Paragraph 1
        delay(3000)
        step = 3 // Paragraph 2
        delay(5000)
        step = 4 // Paragraph 3
        delay(5000)
        step = 5 // Paragraph 4
        delay(3000)
        step = 6 // Paragraph 5
        delay(2000)
        step = 7 // Begin button
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidIndigo)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Starfield()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = step >= 1,
                enter = fadeIn(tween(1500)) + scaleIn(tween(1500), initialScale = 0.8f)
            ) {
                Text(
                    text = "TITAN'S HEART",
                    style = MaterialTheme.typography.displayLarge,
                    color = EmberGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                storyParagraphs.forEachIndexed { index, paragraph ->
                    TypewriterText(
                        text = paragraph,
                        visible = step >= index + 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedVisibility(
                visible = step >= 7,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(1000)) { it / 2 }
            ) {
                ArcaneButton(
                    onClick = onFinished,
                    containerColor = EmberGold
                ) {
                    Text("Begin", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun TypewriterText(text: String, visible: Boolean, modifier: Modifier = Modifier) {
    var displayedText by remember { mutableStateOf("") }
    
    LaunchedEffect(visible) {
        if (visible) {
            text.forEach { char ->
                displayedText += char
                delay(20)
            }
        }
    }

    if (visible || displayedText.isNotEmpty()) {
        Text(
            text = displayedText,
            style = MaterialTheme.typography.bodyLarge,
            color = MoonMist.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = modifier
        )
    }
}
