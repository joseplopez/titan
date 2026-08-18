package com.centelles.titan.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.centelles.titan.BuildConfig
import com.centelles.titan.logic.GameState
import com.centelles.titan.ui.theme.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
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
    contentColor: Color? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    border: BorderStroke? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "buttonScale")
    
    val finalContentColor = contentColor ?: when(containerColor) {
        EmberGold, SpectralCyan -> VoidIndigo
        else -> Color.White
    }
    
    val finalBorder = border ?: if (enabled) BorderStroke(1.5.dp, containerColor.copy(alpha = 0.6f)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = finalBorder,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = finalContentColor,
            disabledContainerColor = MysticBlue.copy(alpha = 0.3f), // More consistent dark base
            disabledContentColor = MoonMist.copy(alpha = 0.4f)
        ),
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
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

    // Pulse animation for ready ads
    val infiniteTransition = rememberInfiniteTransition(label = "adPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    ArcaneButton(
        onClick = onClick,
        enabled = isReady,
        modifier = modifier,
        containerColor = if (isReady) SpectralCyan.copy(alpha = pulseAlpha) else Color(0xFF1A1A1A),
        contentColor = if (isReady) VoidIndigo else Color.White.copy(alpha = 0.5f),
        contentPadding = if (compact) PaddingValues(horizontal = 8.dp, vertical = 4.dp) else PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.PlayArrow, 
                contentDescription = null, 
                modifier = Modifier.size(if (compact) 14.dp else 18.dp), 
                tint = if (isReady) VoidIndigo else Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (isReady) label else "${remainingTime / 1000}s",
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isReady) VoidIndigo else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val adUnitId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/6300978111" // Google Test ID
    } else {
        "ca-app-pub-9749336798654274/9920272911" // Titan Production ID
    }

    // Standard banner height is 50dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    adListener = object : com.google.android.gms.ads.AdListener() {
                        override fun onAdLoaded() {
                            android.util.Log.d("TitanAds", "Banner ad loaded successfully")
                        }

                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            android.util.Log.e("TitanAds", "Banner ad failed to load: ${error.message} (Code: ${error.code})")
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
