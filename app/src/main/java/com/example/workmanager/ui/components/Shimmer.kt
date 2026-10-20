package com.example.workmanager.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    background(brush)
}

@Composable
fun ReminderCardSkeleton() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .shimmerEffect()
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .shimmerEffect()
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .shimmerEffect()
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .shimmerEffect()
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
fun ReminderListSkeletonLoader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(4) {
            ReminderCardSkeleton()
        }
    }
}

@Composable
fun HistorySkeletonLoader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chart Skeleton Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect()
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(7) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height((40..70).random().dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        repeat(3) {
            ReminderCardSkeleton()
        }
    }
}
