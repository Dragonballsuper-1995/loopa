<div align="center">
  <img src="https://raw.githubusercontent.com/Dragonballsuper-1995/loopa/main/website/assets/logo.svg" width="200" alt="Loopa Logo">
  
  <h1>LOOPA</h1>
  
  <p><strong>Your Digital Media Universe. Track, Sync, and Discover.</strong></p>
  
  <p>
    <a href="https://github.com/Dragonballsuper-1995/loopa/releases/latest"><img src="https://img.shields.io/badge/Download-APK-FF4500?style=for-the-badge&logo=android" alt="Download APK" /></a>
    <a href="https://loopa-web.vercel.app"><img src="https://img.shields.io/badge/Visit-Website-00E5FF?style=for-the-badge&logo=vercel" alt="Website" /></a>
  </p>
</div>

<br />

> **Loopa** is a premium, cross-platform media tracker (Movies, TV Shows, Anime) engineered with a brutalist aesthetic (Project "Tsugi"). Powered by Supabase for real-time synchronization and Google Gemini AI for highly personalized recommendations.

---

## ⚡ Key Features

| Feature | Description |
|---|---|
| 🌌 **Premium Cyberpunk UI** | Brutalist styling, skewed neon geometry (`transform: skewX(-3deg)`), glassmorphism, and dynamic 3D parallax hover effects. |
| 🔄 **Real-Time Sync** | Built on **Supabase**. Your watchlist is instantly updated and mirrored across the Web and Android apps. |
| 🧠 **AI Recommendations** | Integrated with **Google Gemini**. A conversational UI that learns your exact tastes and tells you what to watch next. |
| 📱 **Native Android App** | Fully native Jetpack Compose application featuring spring-physics animations, haptic feedback, and custom Aurora shaders. |
| 🌐 **Modern Web App** | Lightning-fast HTML/Tailwind web client with fluid micro-interactions and smooth transitions. |
| 🚨 **Crash & Analytics** | Fully integrated with **Firebase Crashlytics** and **Google Analytics** for bulletproof stability. |

---

## 🎨 Design Philosophy (Tsugi Redesign)

The application embraces a tech-forward, digital-first aesthetic defined by:
- **Palette**: Deep blacks (`#0a0a0a`), elevated charcoals (`#161616`), pierced by vivid **Neon Orange** (`#FF4500`) and **Vibrant Cyan** (`#00E5FF`).
- **Typography**: Heavily reliant on **Bebas Neue** for striking, uppercase headers, paired with **Manrope** for clean, geometric readability.
- **Geometry**: Sharp edges replacing standard Material rounded corners. Elements lean forward slightly, emphasizing speed and raw data.

---

## 🚀 Setup & Installation

### Android App
To run the native Jetpack Compose application:

1. Open the `/app` folder in Android Studio.
2. Ensure you have your `google-services.json` placed inside the `/app` directory.
3. Add a `.env` file in the root containing your `GEMINI_API_KEY` and Supabase keys.
4. Click **Run** on your physical device or emulator. 
   *(Alternatively, grab the latest pre-compiled [Release APK](https://github.com/Dragonballsuper-1995/loopa/releases/latest)).*

### Web Platform
To run the web dashboard:

1. Navigate to the `/website` directory.
2. The site requires no build step—you can serve it using any static server:
   ```bash
   npx serve website
   ```
3. To deploy, simply link the repository to a Vercel or Netlify project pointing to the `/website` root!

---

## 🏗 Architecture & Stack

- **Android Client**: Kotlin, Jetpack Compose, Coroutines/Flow, Retrofit, Ktor.
- **Web Client**: HTML5, Vanilla JavaScript (ESModules), Tailwind CSS.
- **Backend & Auth**: Supabase (PostgreSQL, GoTrue, Realtime).
- **AI Engine**: Google Gemini Pro APIs.
- **Media Data**: TMDB API, Jikan (MyAnimeList) API.

---

<div align="center">
  <sub>Built with ❤️ by Dragonballsuper-1995</sub>
</div>
