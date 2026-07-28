# Visual Identity & Design System: Loopa

## Overview

Loopa reads like the opening credits of an action thriller. The canvas is a deep, cinematic charcoal. The brand voltage relies on the classic Hollywood contrast trick: Amber accents against a dark base, with strict angularity and momentum. 

## 1. Master Design Tokens

The following JSON configuration acts as the **single source of truth** across Web (Tailwind) and Android (Compose).

### Colors
- **Base** (`#0F0E0C` / `loopBase`): Primary canvas / page background.
- **Surface** (`#1A1915` / `loopSurface`): Cards, containers, modals.
- **Raised** (`#242320` / `loopRaised`): Hover states, elevated elements, inputs.
- **Amber** (`#E8A87C` / `loopAmber`): Primary brand accent, CTAs, active states.
- **Amber Strong** (`#D4845A` / `loopAmberStrong`): Pressed states, gradient end.
- **Amber Subtle** (`#2A1F17` / `loopAmberSubtle`): Chip/tag backgrounds, amber container.
- **Text Primary** (`#F0EDE8` / `textPrimary`): Headlines, primary UI text.
- **Text Secondary** (`#A09990` / `textSecondary`): Labels, metadata, secondary text.
- **Text Muted** (`#5C574F` / `textMuted`): Captions, placeholders, disabled text.
- **Success** (`#7AB87A` / `loopSuccess`): Watched / completed state.
- **Error** (`#C87070` / `loopError`): Error, delete, danger actions.

### Borders (Opacity adjustments on Text Primary)
- **Border Subtle**: `#F0EDE8` @ 7% opacity (`rgba(240,237,232,0.071)`) — Hairline dividers.
- **Border Default**: `#F0EDE8` @ 12% opacity (`rgba(240,237,232,0.122)`) — Card borders.
- **Border Strong**: `#F0EDE8` @ 20% opacity (`rgba(240,237,232,0.200)`) — Focus rings.

### Radii
- **Pill**: `999px / 999.dp`
- **Card**: `14px / 14.dp`
- **Badge**: `6px / 6.dp`
- **Input**: `10px / 10.dp`
- **Dialog**: `20px / 20.dp`

### Typography (DM Sans)
*Note: `BebasNeue` has been deprecated in favor of DM Sans.*
- **Font Family**: DM Sans
- **Weights**: 400 (Normal), 500 (Medium), 600 (SemiBold), 700 (Bold), 800 (ExtraBold).
- **Display Large**: `48sp / Bold`
- **Headline Large**: `26sp / SemiBold`
- **Title Large**: `16sp / SemiBold`
- **Body Large**: `16sp / Normal`
- **Label Medium**: `11sp / Medium`

## 2. Layout & Elevation

- **Elevation**: relies on stark contrast and hard, directional solid-color drop shadows instead of soft, feathered blurs.
- **Layout**: Uses masonry and poster-driven grids. Vertical spacing between headers and supporting text is tight to maintain visual cohesion.
- **Dark Mode**: Dark mode is the ONLY supported mode. Light mode is explicitly not supported to preserve the cinematic aesthetic.
