# Loopa — Your Personal Media Universe

Loopa is a modern, cross-platform media tracking application designed to help you beautifully log everything you watch (Movies, TV Shows, Anime). Integrated with a powerful AI recommendation engine, Loopa curates your next obsession based on your exact tastes.

## Features
- **Cross-Platform**: A seamless experience across both Android and the Web.
- **Realtime Sync**: Powered by Supabase, your watchlist updates instantly across all your devices.
- **AI Recommendations**: A conversational AI interface powered by Gemini to help you discover new content.
- **Premium Design**: Warm, immersive UI with haptic feedback, spring physics, and 3D parallax effects.

## Project Structure
- `/app`: The Android application built with Jetpack Compose.
- `/website`: The Web frontend built with HTML, Tailwind CSS, and Vanilla JavaScript.

## Setup Instructions

### Android App
1. Open the `/app` directory in Android Studio.
2. Add your `google-services.json` file in the `app/` folder (for Firebase Analytics/Crashlytics).
3. Build and run on an emulator or physical device.

### Web App
1. Serve the `/website` directory using any local web server (e.g., `npx serve website`).
2. The web app can be deployed seamlessly to Vercel or any static hosting provider.
