package com.loopa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// ── Loopa Design Tokens ───────────────────────────────────────────────────────
object Loopa {
    // v2.1 — Phase 1: deeper surfaces, raised TextMuted, added Terracotta
    val Base       = Color(0xFF0C0B09)
    val Surface    = Color(0xFF1C1A17)
    val Raised     = Color(0xFF2A2823)
    val Amber      = Color(0xFFE8A87C)
    val AmberStrong = Color(0xFFD4845A)
    val AmberSubtle = Color(0xFF2A1F17)
    val Terracotta  = Color(0xFFC47A5A)   // Secondary accent — decorative, non-CTA
    val TextPrimary   = Color(0xFFF0EDE8)
    val TextSecondary = Color(0xFFA09990)
    val TextMuted     = Color(0xFF7A746D)   // Raised from #5C574F for readability
    val Success    = Color(0xFF7AB87A)
    val Error      = Color(0xFFC87070)
    val Border     = Color(0x12F0EDE8)
    val BorderMd   = Color(0x1FF0EDE8)

    val PillShape    = RoundedCornerShape(999.dp)
    val CardShape    = RoundedCornerShape(14.dp)
    val BadgeShape   = RoundedCornerShape(6.dp)
    val InputShape   = RoundedCornerShape(10.dp)
    val DialogShape  = RoundedCornerShape(20.dp)
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. LoopButton — pill-shaped primary / secondary action button
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoopButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val bgColor = if (isSecondary) Loopa.Surface else Loopa.Amber
    val textColor = if (isSecondary) Loopa.TextPrimary else Loopa.Base
    val borderColor = if (isSecondary) Loopa.BorderMd else Color.Transparent

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "button_scale",
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .scale(scale)
            .clip(Loopa.PillShape)
            .background(bgColor)
            .border(1.dp, borderColor, Loopa.PillShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            })
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) leadingIcon()
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. LoopBadge — small rounded label for media type, status, genres
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoopBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Loopa.Surface,
    textColor: Color = Loopa.TextSecondary,
    borderColor: Color = Loopa.BorderMd
) {
    Box(
        modifier = modifier
            .clip(Loopa.BadgeShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, Loopa.BadgeShape)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. LoopCard — rounded surface container for list items
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoopCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = onClick != null && interactionSource.collectIsPressedAsState().value
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "card_scale",
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) Color(0x33F0EDE8) else Loopa.Border,
        label = "card_border"
    )

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = 8.dp, shape = Loopa.CardShape, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.7f))  // Phase 1C
            .clip(Loopa.CardShape)
            .background(Loopa.Surface)
            .border(1.dp, borderColor, Loopa.CardShape)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                ) else Modifier
            )
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. LoopDialogContainer — rounded modal/dialog panel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoopDialogContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .shadow(elevation = 24.dp, shape = Loopa.DialogShape, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.85f))  // Phase 1C
            .clip(Loopa.DialogShape)
            .background(Loopa.Surface)
            .border(1.dp, Loopa.Border, Loopa.DialogShape)
    ) {
        // Amber accent top bar (subtle, not neon)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(listOf(Loopa.Amber, Loopa.AmberStrong))
                )
        )
        Box { content() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. LoopEmptyState — soft rounded empty state box
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoopEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(Loopa.CardShape)
            .background(Loopa.Surface)
            .border(1.dp, Loopa.Border, Loopa.CardShape)
            .padding(vertical = 48.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Loopa.TextMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. LoopPosterCard — media poster card with warm rounded design
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoopPosterCard(
    title: String,
    imageUrl: String?,
    mediaType: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    onRelease: (() -> Unit)? = null,
    score: Double? = null,
    statusLabel: String? = null,
    progressText: String? = null,
    totalEpisodes: Int = 0,
    currentEpisode: Int = 0,
    isAiMatch: Boolean = false
) {
    // Phase 1B: type badge is always neutral — amber is reserved for CTAs
    val typeColor = Loopa.TextSecondary

    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "poster_scale",
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    val currentOnClick = rememberUpdatedState(onClick)
    val currentOnLongPress = rememberUpdatedState(onLongPress)
    val currentOnRelease = rememberUpdatedState(onRelease)

    Box(
        modifier = modifier
            .scale(scale)
            .aspectRatio(2f / 3f)
            .clip(Loopa.CardShape)
            .background(Loopa.Surface)
            .border(1.dp, Loopa.Border, Loopa.CardShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnClick.value() 
                    },
                    onLongPress = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnLongPress.value?.invoke() 
                    },
                    onPress = {
                        isPressed = true
                        try { awaitRelease() } finally { 
                            isPressed = false
                            currentOnRelease.value?.invoke() 
                        }
                    }
                )
            }
    ) {
        // Poster Image
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Loopa.Raised))
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.5f to Color.Black.copy(alpha = 0.2f),
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Top badges row
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            if (isAiMatch) {
                Box(
                    modifier = Modifier
                        .clip(Loopa.BadgeShape)
                        .background(Loopa.Amber)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "AI Match",
                        color = Loopa.Base,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (score != null && score > 0.0) {
                Box(
                    modifier = Modifier
                        .clip(Loopa.BadgeShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "★ ${String.format("%.1f", score)}",
                        color = Loopa.Amber,
                        fontSize = 12.sp,   // Phase 1B: bumped from 9sp for readability
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (!statusLabel.isNullOrBlank()) {
                val isWatching = statusLabel.contains("Watching", ignoreCase = true)
                val bg = if (isWatching) Loopa.AmberSubtle.copy(alpha = 0.9f) else Loopa.Surface.copy(alpha = 0.9f)
                val border = if (isWatching) Loopa.Amber.copy(alpha = 0.35f) else Loopa.BorderMd
                val textColor = if (isWatching) Loopa.Amber else Loopa.TextSecondary
                val icon = if (isWatching) "● " else "✓ "

                Box(
                    modifier = Modifier
                        .clip(Loopa.PillShape)
                        .background(bg)
                        .border(1.dp, border, Loopa.PillShape)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$icon${if (isWatching) "Watching" else "Watched"}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }
        }

        // Bottom info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LoopBadge(
                    text = mediaType.uppercase(),
                    backgroundColor = Color.Black.copy(alpha = 0.6f),
                    textColor = typeColor,
                    borderColor = typeColor.copy(alpha = 0.4f)
                )
                if (!progressText.isNullOrBlank()) {
                    Text(
                        text = progressText,
                        color = Loopa.Amber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Loopa.TextPrimary,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (totalEpisodes > 0 && currentEpisode > 0) {
            val progress = (currentEpisode.toFloat() / totalEpisodes.toFloat()).coerceIn(0f, 1f)
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp),
                color = Loopa.Amber,
                trackColor = Loopa.Base.copy(alpha = 0.5f)
            )
        }
    }
}

// ── Compatibility shims for code that still uses Loopa* names ────────────────
@Composable
fun LoopaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isSecondary: Boolean = false) =
    LoopButton(text = text, onClick = onClick, modifier = modifier, isSecondary = isSecondary)

@Composable
fun LoopaBadge(text: String, modifier: Modifier = Modifier, color: Color = Loopa.TextSecondary) =
    LoopBadge(text = text, modifier = modifier, textColor = color, borderColor = color.copy(0.4f))

@Composable
fun LoopaCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) =
    LoopCard(modifier = modifier, onClick = onClick, content = content)

@Composable
fun LoopaDialogContainer(modifier: Modifier = Modifier, accentColor: Color = Loopa.Amber, content: @Composable () -> Unit) =
    LoopDialogContainer(modifier = modifier, content = content)

@Composable
fun LoopaEmptyState(message: String, modifier: Modifier = Modifier) =
    LoopEmptyState(message = message, modifier = modifier)

@Composable
fun LoopaPosterCard(
    title: String, imageUrl: String?, mediaType: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, onLongPress: (() -> Unit)? = null, onRelease: (() -> Unit)? = null,
    score: Double? = null, statusLabel: String? = null, statusColor: Color = Loopa.Amber, progressText: String? = null,
    totalEpisodes: Int = 0, currentEpisode: Int = 0
) = LoopPosterCard(
    title = title, imageUrl = imageUrl, mediaType = mediaType, onClick = onClick,
    modifier = modifier, onLongPress = onLongPress, onRelease = onRelease,
    score = score, statusLabel = statusLabel, progressText = progressText,
    totalEpisodes = totalEpisodes, currentEpisode = currentEpisode
)

// Keep SkewedShape as a no-op rounded shape so usages don't crash at compile time
class SkewedShape(private val skewFactor: Float = 0.08f) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        return RoundedCornerShape(10.dp).createOutline(size, layoutDirection, density)
    }
}

@Composable
fun LoopStatsBanner(statsState: com.loopa.viewmodel.StatsState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LoopStatsCard(
            modifier = Modifier.weight(1f),
            title = "Time Watched",
            value = statsState.totalWatchTimeStr,
            icon = androidx.compose.material.icons.Icons.Default.PlayArrow
        )
        LoopStatsCard(
            modifier = Modifier.weight(1f),
            title = "Top Genre",
            value = statsState.topGenre,
            icon = androidx.compose.material.icons.Icons.Default.Star
        )
        LoopStatsCard(
            modifier = Modifier.weight(1f),
            title = "Total Titles",
            value = statsState.totalTitles.toString(),
            icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.List
        )
        LoopStatsCard(
            modifier = Modifier.weight(1f),
            title = "Top Creator",
            value = statsState.topDirector,
            icon = androidx.compose.material.icons.Icons.Default.AccountCircle
        )
    }
}

@Composable
fun LoopStatsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Loopa.Surface.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { }
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Loopa.TextSecondary,   // Phase 1B: neutral icon, not amber
                modifier = Modifier.size(24.dp).padding(bottom = 4.dp)
            )
            Text(
                text = title.uppercase(),
                color = Loopa.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                color = Loopa.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
