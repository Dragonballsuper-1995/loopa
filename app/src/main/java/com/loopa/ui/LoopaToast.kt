package com.loopa.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

// ─────────────────────────────────────────────────────────────────────────────
// LoopToastHost — warm amber pill toast notification.
//   Matches website: fixed top, rounded-full, loopSurface bg, amber border & icon.
//   Place once inside MediaTrackerApp's root Box at the highest z-order.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoopToastHost(toastFlow: SharedFlow<String>) {
    var message by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(toastFlow) {
        toastFlow.collect { msg ->
            message = msg
            visible = true
            delay(2500)
            visible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(animationSpec = tween(300), initialOffsetY = { -it }),
            exit  = slideOutVertically(animationSpec = tween(250), targetOffsetY = { -it })
        ) {
            Box(
                modifier = Modifier
                    .clip(Loopa.PillShape)
                    .background(Loopa.Surface)
                    .border(1.dp, Loopa.Amber.copy(alpha = 0.35f), Loopa.PillShape)
                    .padding(horizontal = 20.dp, vertical = 11.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Loopa.Amber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = message,
                        color = Loopa.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    )
                }
            }
        }
    }
}

// Compatibility shim — LoopaToastHost still works
@Composable
fun LoopaToastHost(toastFlow: SharedFlow<String>) = LoopToastHost(toastFlow)
