package com.example.workmanager.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "ambient_mesh")
    val offsetAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_offset"
    )

    val primaryContainer = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.20f)
    val surfaceColor = MaterialTheme.colorScheme.background

    val brush = Brush.radialGradient(
        colors = listOf(primaryContainer, tertiaryContainer, surfaceColor),
        center = Offset(offsetAnim, offsetAnim / 2),
        radius = 1200f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
            .background(brush)
    ) {
        content()
    }
}
