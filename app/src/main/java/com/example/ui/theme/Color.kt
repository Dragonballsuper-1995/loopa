package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── Loopa v2.0 Color System ───────────────────────────────────────────────────
// Design: Warm dark palette + single Loopa Amber accent

// Canvas / Background layers
val LoopBase    = Color(0xFF0F0E0C)   // Primary canvas — warm near-black
val LoopSurface = Color(0xFF1A1915)   // Cards, containers
val LoopRaised  = Color(0xFF242320)   // Hover states, elevated elements

// Brand Accent — Loopa Amber
val LoopAmber       = Color(0xFFE8A87C)   // Primary accent
val LoopAmberStrong = Color(0xFFD4845A)   // Active / pressed states
val LoopAmberSubtle = Color(0xFF2A1F17)   // Chip / tag backgrounds

// Foreground / Typography
val TextPrimary   = Color(0xFFF0EDE8)   // Warm off-white — headlines, primary UI
val TextSecondary = Color(0xFFA09990)   // Warm medium gray — labels, meta
val TextMuted     = Color(0xFF5C574F)   // Low-emphasis — captions, placeholders

// Semantic
val LoopSuccess = Color(0xFF7AB87A)   // Watched state
val LoopError   = Color(0xFFC87070)   // Error / remove state

// Borders
val BorderSubtle  = Color(0x12F0EDE8)   // Hairline dividers
val BorderDefault = Color(0x1FF0EDE8)   // Card borders
val BorderStrong  = Color(0x33F0EDE8)   // Focus rings

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
val md_theme_dark_scrim               = Color(0xCC0F0E0C)
