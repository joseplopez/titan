package com.centelles.titan.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centelles.titan.logic.LAYERS
import com.centelles.titan.ui.theme.MoonMist
import com.centelles.titan.ui.theme.VoidIndigo
import kotlinx.coroutines.delay

@Composable
fun LayerIntroScreen(layer: Int, onFinished: () -> Unit) {
    val layerDef = LAYERS.find { it.level == layer } ?: LAYERS.first()
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(2000))
        delay(4000)
        // Auto-dismiss or wait for button? The prompt says "Continue" button
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VoidIndigo.copy(alpha = 0.9f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Starfield()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Layer $layer",
                    color = MoonMist.copy(alpha = 0.7f * alpha.value),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 4.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = layerDef.name,
                    color = Color.White.copy(alpha = alpha.value),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = layerDef.flavor,
                    color = MoonMist.copy(alpha = alpha.value),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                if (alpha.value > 0.8f) {
                    Button(
                        onClick = onFinished,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Descend Deeper", color = Color.White)
                    }
                }
            }
        }
    }
}
