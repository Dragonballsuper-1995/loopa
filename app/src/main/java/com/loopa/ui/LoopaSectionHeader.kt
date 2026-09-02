package com.loopa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// LoopSectionHeader — left-accent section header matching Loopa website style.
//   Supports highlighting keywords in Loopa Amber.
//
// Usage:
//   LoopSectionHeader("Trending", highlightPrefix = "Trending", onSeeAll = { ... })
//   LoopSectionHeader("Top 10 Today", highlightPrefix = "Top 10")
//   LoopSectionHeader("Popular Movies", highlightPrefix = "Popular")
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LoopSectionHeader(
    title: String,
    highlightPrefix: String? = null,
    accentColor: Color = Loopa.Amber,
    subtitle: String? = null,
    titleSize: Int = 18,
    showDivider: Boolean = false,
    onSeeAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val annotatedTitle = remember(title, highlightPrefix, accentColor) {
        buildAnnotatedString {
            if (!highlightPrefix.isNullOrEmpty() && title.startsWith(highlightPrefix, ignoreCase = true)) {
                val prefix = title.substring(0, highlightPrefix.length)
                val rest = title.substring(highlightPrefix.length)
                withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                    append(prefix)
                }
                if (rest.isNotEmpty()) {
                    withStyle(SpanStyle(color = Loopa.TextPrimary, fontWeight = FontWeight.Bold)) {
                        append(rest)
                    }
                }
            } else {
                withStyle(SpanStyle(color = Loopa.TextPrimary, fontWeight = FontWeight.Bold)) {
                    append(title)
                }
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column {
                Text(
                    text = annotatedTitle,
                    fontSize = titleSize.sp,
                    lineHeight = (titleSize * 1.3f).sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                }
            }

            if (onSeeAll != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onSeeAll() }.padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Text(
                        text = "See All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Loopa.TextMuted
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Loopa.TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        if (showDivider) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = Loopa.Border,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// Compatibility shim — existing code using LoopaSectionHeader still works
@Composable
fun LoopaSectionHeader(
    title: String,
    highlightPrefix: String? = null,
    accentColor: Color = Loopa.Amber,
    subtitle: String? = null,
    titleSize: Int = 18,
    showDivider: Boolean = false,
    onSeeAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) = LoopSectionHeader(
    title = title,
    highlightPrefix = highlightPrefix,
    accentColor = Loopa.Amber,
    subtitle = subtitle,
    titleSize = titleSize,
    showDivider = showDivider,
    onSeeAll = onSeeAll,
    modifier = modifier
)
