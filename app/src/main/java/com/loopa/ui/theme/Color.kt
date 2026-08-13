package com.loopa.ui.theme

import androidx.compose.ui.graphics.Color

// ── Loopa v2.1 Color System ───────────────────────────────────────────────────
// Design: Warm dark palette + single Loopa Amber accent + Terracotta secondary
// Phase 1 update: deeper surface layers, raised TextMuted for accessibility

// Canvas / Background layers
val LoopBase    = Color(0xFF0C0B09)   // Primary canvas — warm near-black (deepened)
val LoopSurface = Color(0xFF1C1A17)   // Cards, containers (warmer mid-dark)
val LoopRaised  = Color(0xFF2A2823)   // Hover states, elevated elements (more contrast)

// Brand Accent — Loopa Amber (CTA-only)
val LoopAmber       = Color(0xFFE8A87C)   // Primary accent — buttons, active states, ratings
val LoopAmberStrong = Color(0xFFD4845A)   // Active / pressed states
val LoopAmberSubtle = Color(0xFF2A1F17)   // Chip / tag backgrounds

// Secondary Accent — Terracotta (non-CTA highlights)
val LoopTerracotta  = Color(0xFFC47A5A)   // Secondary accent for decorative / non-interactive highlights

// Foreground / Typography
val TextPrimary   = Color(0xFFF0EDE8)   // Warm off-white — headlines, primary UI
val TextSecondary = Color(0xFFA09990)   // Warm medium gray — labels, meta
val TextMuted     = Color(0xFF7A746D)   // Low-emphasis — captions, placeholders (raised for readability)

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
val md_theme_dark_scrim               = Color(0xCC0C0B09)
