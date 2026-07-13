<div align="center">
  <img src="website/assets/logo.svg" width="200" alt="Loopa Logo">
  
  # Loopa
  **Discover Your Next Obsession.**
  
  [![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
  [![Web](https://img.shields.io/badge/Platform-Web-000000?style=for-the-badge&logo=firebase&logoColor=white)](#)
  [![Supabase](https://img.shields.io/badge/Database-Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)](#)
  
  *A cross-platform, AI-powered media tracking universe.*

  [**Explore the Web App (Live)**](https://loopa-4e92d.web.app/) | [**Download Android APK**](https://github.com/Dragonballsuper-1995/loopa/releases)
  
  *Mirrors: [Netlify](https://loopa1.netlify.app/) | [Vercel](https://loopa1.vercel.app/) | [GitHub Pages](https://dragonballsuper-1995.github.io/loopa/)*
</div>

<br>

---

## What is Loopa?

**Loopa** is a modern, premium application designed to help you beautifully log everything you watch—movies, TV shows, and anime—and let a conversational AI curate your next obsession. 

Featuring a highly stylized **warm, minimalistic design**, Loopa strips away visual clutter in favor of a clean, editorial UI with **organic pill shapes, amber accents, and elegant glassmorphic overlays**. 

Whether you're sitting at your desktop browsing on the Web, or swiping on the Android app, your watchlist updates **instantly** via Realtime Supabase syncing.

---

## Core Features

- **AI-Powered Discovery:** Chat with an integrated conversational AI (powered by Google Gemini) to get hyper-personalized recommendations based on your tastes.
- **Realtime Cross-Platform Sync:** Start logging a movie on the web, and watch it instantly appear on your Android device. Data is powered by a robust Supabase backend.
- **Premium Aesthetic & UX:**
  - **Android:** Custom canvas-drawn shapes, fluid spring physics, and a deeply warm dark-mode UI.
  - **Web:** Glassmorphic overlays, clean DM Sans typography, and fluid CSS transitions for a seamless desktop experience.
- **Firebase Analytics & Crashlytics:** Enterprise-grade crash reporting and usage analytics built directly into the Android client.

---

## Platforms

### 1. Android (Native Kotlin / Jetpack Compose)
The Android app is built entirely natively using the latest declarative UI framework from Google. It completely overhauls the Material 3 standard in favor of a bespoke visual language emphasizing organic geometry, pill shapes, and a warm CineCharcoal canvas.

* **Source Location:** `/app` directory
* **Tech Stack:** Kotlin, Jetpack Compose, Ktor, Supabase Android SDK, Room DB, Firebase Crashlytics.
* **Building it locally:**
  1. Open the `/app` folder in Android Studio.
  2. Place your `google-services.json` inside the `app/` folder.
  3. Ensure your `.env` contains the required Supabase and Gemini keys.
  4. Hit **Run** or build `loopa.apk` via `./gradlew assembleRelease`.

### 2. Web (HTML / Tailwind CSS / Vanilla JS)
The Web dashboard provides a lightweight, blisteringly fast desktop experience.

* **Source Location:** `/website` directory
* **Tech Stack:** HTML5, Tailwind CSS (via CDN), Vanilla JavaScript, Supabase JS SDK, Firebase Web SDK.
* **Running it locally:**
  1. Navigate to `/website`.
  2. Serve locally: `npx serve .` or `python -m http.server 8000`.
  3. Deploy instantly to Firebase Hosting, Netlify, or Vercel.

---

## Design Language & Visual Tokens

Loopa strictly follows a warm, minimal identity:
* **Backgrounds:** `CineCharcoal` (#0F0E0C) and `CineSurface` (#1A1915).
* **Accents:** `Amber` (#E8A87C) for primary actions and highlights.
* **Typography:** `DM Sans` for clean, modern, and highly legible text. We favor lowercase styling for a friendly, approachable feel.
* **Geometry:** Organic pill shapes, soft borders, and deep tonal contrasts instead of drop shadows.

---

## Getting Started

If you are cloning this repository to build locally, you will need a few secrets:

1. **Supabase:** Create a Supabase project, enable Email Auth, and copy your `URL` and `ANON_KEY`.
2. **Gemini AI:** Get a free API key from Google AI Studio.
3. **TMDB:** Get a free API key from The Movie Database.

Place these in an `.env` file for Android, and in `website/index.html` for the web!

<br>

<div align="center">
  <sub>Built with care by Dragonballsuper-1995.</sub>
</div>
