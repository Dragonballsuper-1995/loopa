<div align="center">
  <img src="website/assets/logo.svg" width="200" alt="Loopa Logo">
  
  # Loopa.
  **Discover Your Next Obsession.**
  
  [![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
  [![Web](https://img.shields.io/badge/Platform-Web-000000?style=for-the-badge&logo=vercel&logoColor=white)](#)
  [![Supabase](https://img.shields.io/badge/Database-Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)](#)
  
  *A cross-platform, AI-powered media tracking universe.*

  [**Explore the Web App (Live)**](https://placeholder-website-link.com) | [**Download Android APK**](https://github.com/Dragonballsuper-1995/loopa/releases/download/v1.0.0/loopa.apk)
</div>

<br>

---

## ⚡ What is Loopa?

**Loopa** (formerly known as ContentGram / Tsugi) is a modern, premium application designed to help you beautifully log everything you watch—Movies, TV Shows, and Anime—and let a conversational AI curate your next obsession. 

Featuring a highly stylized **cyberpunk, brutalist design**, Loopa strips away standard rounded-corner templates in favor of a dynamic, edge-forward UI with **skewed parallelograms, neon-accented glows, and 3D parallax hover effects**. 

Whether you're sitting at your desktop browsing on the Web, or swiping on the Android app, your watchlist updates **instantly** via Realtime Supabase syncing.

---

## ✨ Core Features

- 🧠 **AI-Powered Discovery:** Chat with an integrated conversational AI (powered by Google Gemini) to get hyper-personalized recommendations based on your tastes.
- 🔄 **Realtime Cross-Platform Sync:** Start logging a movie on the web, and watch it instantly appear on your Android device. Data is powered by a robust Supabase backend.
- 🎨 **Premium Aesthetic & UX:**
  - **Android:** Custom canvas-drawn shapes, Haptic feedback engines, fluid Spring physics, and a custom Aurora Shader backdrop.
  - **Web:** Glassmorphic overlays, 3D pointer-tracking parallax on movie posters, and fluid CSS transitions.
- 📊 **Firebase Analytics & Crashlytics:** Enterprise-grade crash reporting and usage analytics built directly into the Android client.

---

## 📱 Platforms

### 1. Android (Native Kotlin / Jetpack Compose)
The Android app is built entirely natively using the latest declarative UI framework from Google. It completely overhauls the Material 3 standard in favor of a bespoke visual language (Bebas Neue fonts, sharp edges, and skewed modifier paths).

* **Source Location:** `/app` directory
* **Tech Stack:** Kotlin, Jetpack Compose, Ktor, Supabase Android SDK, Room DB, Firebase Crashlytics.
* **Building it locally:**
  1. Open the `/app` folder in Android Studio.
  2. Place your `google-services.json` inside the `app/` folder.
  3. Ensure your `.env` contains the required Supabase and Gemini keys.
  4. Hit **Run** or build `loopa.apk` via `./gradlew assembleRelease`.

### 2. Web (HTML / Tailwind CSS / Vanilla JS)
The Web dashboard provides a lightweight, blisteringly fast desktop experience with 3D interactions.

* **Source Location:** `/website` directory
* **Tech Stack:** HTML5, Tailwind CSS (via CDN), Vanilla JavaScript, Supabase JS SDK, Firebase Web SDK.
* **Running it locally:**
  1. Navigate to `/website`.
  2. Serve locally: `npx serve .` or `python -m http.server 8000`.
  3. Deploy instantly to Vercel or any static host.

---

## 🎨 Design Language & Visual Tokens

Loopa strictly follows a cyberpunk/neon-brutalist identity:
* **Backgrounds:** `CineCharcoal` (#0a0a0a) and `CineSurface` (#161616).
* **Accents:** `NeonOrange` (#FF4500) for primary actions and `VibrantCyan` (#00E5FF) for secondary highlights.
* **Typography:** `Bebas Neue` for massive, condensed, uppercase headers. `Manrope` for legible, geometric body text.
* **Geometry:** Sharp edges, `skewX(-3deg)` transform logic on cards to invoke speed and digital distortion, and heavy use of `mix-blend-screen` gradients.

---

## 🚀 Getting Started

If you are cloning this repository to build locally, you will need a few secrets:

1. **Supabase:** Create a Supabase project, enable Email Auth, and copy your `URL` and `ANON_KEY`.
2. **Gemini AI:** Get a free API key from Google AI Studio.
3. **TMDB:** Get a free API key from The Movie Database.

Place these in an `.env` file for Android, and in `website/js/config.js` for the web!

<br>

<div align="center">
  <sub>Built with ❤️ by Dragonballsuper-1995.</sub>
</div>
