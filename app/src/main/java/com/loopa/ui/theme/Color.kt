package com.loopa.ui.theme

import androidx.compose.ui.graphics.Color

// ── Loopa v3.0 Color System (Industry-Leading Obsidian & Electric Coral) ───────
// Canvas / Background layers
val LoopBase    = Color(0xFF060608)   // Ultra-deep obsidian canvas
val LoopSurface = Color(0xFF101014)   // Cards, panels
val LoopRaised  = Color(0xFF181820)   // Elevated elements, hover states

// Brand Accent — Electric Sunset Coral / Primary CTA
val LoopAmber       = Color(0xFFFF5A36)   // Primary accent — buttons, active states
val LoopAmberStrong = Color(0xFFE04826)   // Active / pressed states
val LoopAmberSubtle = Color(0xFF26120E)   // Chip / tag background tint

// Secondary Accents & Badges
val LoopTerracotta  = Color(0xFFE88A6E)   // Secondary warm accent
val LoopGold        = Color(0xFFFFB800)   // Star rating & awards
val LoopCyan        = Color(0xFF00D2FF)   // Tech specs & anime badges
val LoopPurple      = Color(0xFF8A4FFF)   // AI Concierge accent

// Foreground / Typography
val TextPrimary   = Color(0xFFFFFFFF)   // Pure crisp white
val TextSecondary = Color(0xFFA3A3B2)   // Refined neutral gray
val TextMuted     = Color(0xFF686878)   // Accessible meta text

// Semantic
val LoopSuccess = Color(0xFF4ADE80)   // Watched state
val LoopError   = Color(0xFFF87171)   // Error / remove state

// Borders
val BorderSubtle  = Color(0x12FFFFFF)   // Hairline dividers
val BorderDefault = Color(0x1FFFFFFF)   // Card borders
val BorderStrong  = Color(0x38FFFFFF)   // Focus rings / active strokes

// ── Material You Mappings (Dark Theme) ───────────────────────────────────────
val md_theme_dark_primary             = LoopAmber
val md_theme_dark_onPrimary           = LoopBase
val md_theme_dark_primaryContainer    = LoopAmberSubtle
val md_theme_dark_onPrimaryContainer  = LoopAmber
val md_theme_dark_secondary           = LoopSurface
val md_theme_dark_onSecondary         = TextPrimary
val md_theme_dark_secondaryContainer  = LoopRaised
val md_theme_dark_onSecondaryContainer = TextSecondary
val md_theme_dark_tertiary            = TextSecondary
val md_theme_dark_onTertiary          = LoopBase
val md_theme_dark_tertiaryContainer   = LoopRaised
val md_theme_dark_onTertiaryContainer = TextPrimary
val md_theme_dark_error               = LoopError
val md_theme_dark_onError             = LoopBase
val md_theme_dark_errorContainer      = Color(0xFF3B1515)
val md_theme_dark_onErrorContainer    = LoopError
val md_theme_dark_outline             = BorderSubtle
val md_theme_dark_background          = LoopBase
val md_theme_dark_onBackground        = TextPrimary
val md_theme_dark_surface             = LoopSurface
val md_theme_dark_onSurface           = TextPrimary
val md_theme_dark_surfaceVariant      = LoopRaised
val md_theme_dark_onSurfaceVariant    = TextSecondary
val md_theme_dark_inverseSurface      = TextPrimary
val md_theme_dark_inverseOnSurface    = LoopBase
val md_theme_dark_inversePrimary      = LoopAmberStrong
val md_theme_dark_surfaceTint         = LoopAmber
val md_theme_dark_outlineVariant      = BorderSubtle
val md_theme_dark_scrim               = Color(0xCC060608)
