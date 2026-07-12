## **version: alpha name: Tsugi description: A fast, cinematic, action-oriented aesthetic that leans heavily into angular momentum. It uses bold orange-and-teal Hollywood color grading and aggressive, skewed typography to feel like an adrenaline-pumping blockbuster. colors: primary: "\#FF4500" secondary: "\#00E5FF" canvas: "\#121212" surface-card: "\#1E1E1E" ink: "\#FFFFFF" muted: "\#9CA3AF" hairline: "rgba(255, 255, 255, 0.1)" on-primary: "\#FFFFFF" typography: display-lg: fontFamily: "'Bebas Neue', sans-serif" fontSize: 48px fontWeight: 400 lineHeight: 1 letterSpacing: 1px headline-md: fontFamily: "'Bebas Neue', sans-serif" fontSize: 24px fontWeight: 400 lineHeight: 1.1 letterSpacing: 0.5px body-md: fontFamily: "'Manrope', sans-serif" fontSize: 14px fontWeight: 600 lineHeight: 1.5 letterSpacing: 0 label: fontFamily: "'Manrope', sans-serif" fontSize: 10px fontWeight: 800 lineHeight: 1 letterSpacing: 1px rounded: none: 0px sm: 2px md: 4px lg: 8px full: 9999px spacing: xs: 4px sm: 8px md: 16px base: 24px lg: 32px xl: 48px section: 64px components: button-initiate: backgroundColor: "{colors.primary}" textColor: "{colors.on-primary}" typography: "{typography.headline-md}" rounded: "{rounded.none}" padding: 12px 32px poster-card: backgroundColor: "{colors.surface-card}" textColor: "{colors.ink}" rounded: "{rounded.md}" padding: 0px**

## **Overview**

Tsugi reads like the opening credits of an action thriller. The canvas is a deep, cinematic charcoal {colors.canvas} (\#121212). The brand voltage relies on the classic Hollywood contrast trick: Neon Orange {colors.primary} (\#FF4500) representing heat and action, against Vibrant Cyan {colors.secondary} (\#00E5FF) representing data and UI elements.

**Key Characteristics:**

* **Momentum through Skew:** Elements are frequently skewed (e.g., transform: skewX(-5deg)). Buttons, tags, and even some images lean forward to create a sense of relentless speed.  
* **Cinematic Type:** Bebas Neue is used exclusively for headers. It is tall, condensed, and aggressive.  
* **Sharp Edges:** Border radii are kept extremely low ({rounded.md} is only 4px). The design avoids soft, bubbly aesthetics entirely.

## **Colors**

### **Brand & Accent**

* **Neon Orange** ({colors.primary} — \#FF4500): The color of initiation. Used for primary CTAs, active states, and major brand moments.  
* **Vibrant Cyan** ({colors.secondary} — \#00E5FF): The color of information. Used for secondary metadata tags, subtle borders, and inactive-but-important UI elements.

### **Surface**

* **Canvas** ({colors.canvas} — \#121212): A pure, matte charcoal. Not quite black, giving the feel of a dark theater room.  
* **Surface Card** ({colors.surface-card} — \#1E1E1E): A slight step up in brightness to separate interactive cards from the background canvas.

### **Hairlines**

* **Hairline** ({colors.hairline} — rgba(255, 255, 255, 0.1)): Subtle demarcations used sparingly to separate sections without drawing attention.

### **Text**

* **Ink** ({colors.ink} — \#FFFFFF): Crisp, stark white for headlines.  
* **Muted** ({colors.muted} — \#9CA3AF): Used for body copy and non-essential meta-data.  
* **On Primary** ({colors.on-primary} — \#FFFFFF): Text sitting on the Neon Orange buttons remains white for a bold, aggressive contrast.

## **Typography**

| Role | Font | Size | Weight | Tracking |
| :---- | :---- | :---- | :---- | :---- |
| display-lg | Bebas Neue | 48px | 400 | 1px |
| headline-md | Bebas Neue | 24px | 400 | 0.5px |
| body-md | Manrope | 14px | 600 | 0 |
| label | Manrope | 10px | 800 | 1px |

### **Principles**

Headings use Bebas Neue—it only has one weight and is always uppercase. It operates like a movie title. Body text uses Manrope at a notably heavy base weight (600) to ensure it holds its own against the aggressive headers and bright neon colors.

### **Note on Font Substitutes**

If Bebas Neue is unavailable, substitute with Oswald or Anton.

## **Layout**

The layout is heavily masonry and poster-driven.

Because of the heavy, condensed fonts, vertical spacing {spacing.sm} between headers and supporting text is kept very tight to group them visually.

## **Elevation**

Elevation relies on stark contrast and harsh, solid-color drop shadows rather than soft blurs.

A hovering card shouldn't have a soft, feathered shadow. It should have a hard, directional shadow (e.g., box-shadow: 4px 4px 0px {colors.primary}) to reinforce the graphic, comic-book/anime action aesthetic.

## **Components**

**button-initiate** — The aggressive primary CTA. Background {colors.primary}, text {colors.on-primary}, type {typography.headline-md} (always uppercase), padding 12px × 32px, completely sharp corners {rounded.none}. Crucially, the CSS should apply a \-5deg skew to the button container, and a \+5deg skew to the text inside to keep the text upright.

**poster-card** — The default container for movie/anime artwork. Background {colors.surface-card}, rounded slightly {rounded.md} (4px). Padding is 0px because the image should bleed to the absolute edges.

## **Responsive Behavior**

| Name | Width | Key Changes |
| :---- | :---- | :---- |
| Mobile | \< 720px | Full width edge-to-edge imagery for hero sections. |
| Tablet | 720–1024px | Grid shifts to 3-column. |
| Desktop | \> 1024px | Skewed elements become more pronounced on hover states. |

### **Touch Targets**

* Due to the sharp, non-rounded aesthetic, tap targets rely on generous padding rather than visual bounding boxes.

### **Collapsing Strategy**

* Typography scales aggressively. The display-lg drops from 48px to 36px on very small viewports to prevent wrapping.

## **Known Gaps**

* The mathematical calculations required to prevent skewed containers (transform: skewX) from cutting off internal text at unexpected viewport widths are left to the implementation layer.  
* Dark mode is the only mode. Light mode for this aesthetic is explicitly not supported.