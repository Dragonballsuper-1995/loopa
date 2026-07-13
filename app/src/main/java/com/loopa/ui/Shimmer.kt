package com.loopa.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    this.then(
        Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF161616),
                    Color(0xFF2A2A2A),
                    Color(0xFF161616),
                ),
                start = Offset(translateAnim, translateAnim),
                end = Offset(translateAnim + 500f, translateAnim + 500f)
            )
        )
    )
}
