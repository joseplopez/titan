package com.centelles.titan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.centelles.titan.logic.GameState
import com.centelles.titan.ui.theme.ArcanePurple
import com.centelles.titan.ui.theme.MoonMist
import com.centelles.titan.ui.theme.SpectralCyan
import com.centelles.titan.ui.theme.VoidIndigo
import kotlinx.coroutines.delay

@Composable
fun ArcanePanel(
    modifier: Modifier = Modifier,
    borderColor: Color = SpectralCyan.copy(alpha = 0.5f),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ArcanePurple.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, borderColor),
        content = content
    )
}

@Composable
fun ArcaneButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = VoidIndigo,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "buttonScale")

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun AdButton(
    label: String,
    cooldownKey: String,
    state: GameState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    val lastWatchTime = state.lastAdWatchTimes.getOrDefault(cooldownKey, 0L)
    val cooldownDuration = 180000L // 3 minutes
    var remainingTime by remember(lastWatchTime) {
        mutableStateOf(maxOf(0L, cooldownDuration - (System.currentTimeMillis() - lastWatchTime)))
    }

    LaunchedEffect(lastWatchTime) {
        while (remainingTime > 0) {
            delay(1000)
            remainingTime = maxOf(0L, cooldownDuration - (System.currentTimeMillis() - lastWatchTime))
        }
    }

    val isReady = remainingTime <= 0L

    ArcaneButton(
        onClick = onClick,
        enabled = isReady,
        modifier = modifier,
        containerColor = if (isReady) SpectralCyan else Color.Gray.copy(alpha = 0.3f),
        contentPadding = if (compact) PaddingValues(horizontal = 8.dp, vertical = 4.dp) else PaddingValues(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isReady) VoidIndigo else MoonMist)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (isReady) label else "${remainingTime / 1000}s",
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = if (isReady) VoidIndigo else MoonMist
            )
        }
    }
}
