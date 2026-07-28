# Context & Analysis: Loopa

## 1. Executive Summary

**Loopa** is a cross-platform (Web + Android) media tracking application that allows users to seamlessly log movies, TV shows, and anime. It features a unified Realtime Supabase syncing protocol and leverages Google Gemini for highly personalized AI recommendations.

Following a comprehensive cross-platform audit, Loopa is now undergoing an implementation roadmap focused on three phases:
- **Phase 1: Database Schema & Sync Protocol Hardening**
- **Phase 2: Visual Design System Synchronization**
- **Phase 3: Feature Parity Acceleration**

## 2. Core Identity

Loopa's aesthetic is characterized by a cinematic, fast, and action-oriented design, using angular momentum, skewed typography, and sharp, high-contrast borders.
- **Platforms**: Android (Native Kotlin/Jetpack Compose) and Web (HTML/JS/Tailwind CSS).
- **Backend**: Supabase (PostgreSQL) with Realtime subscriptions.
- **AI Integration**: Google Gemini via a Cloudflare Worker proxy (`https://loopa-ai-proxy.sujalsanjay-chhajed2023.workers.dev`).

## 3. Platform Architectures

### 3.1 Android (Native)
Built with **Jetpack Compose** using a modern MVVM architecture.
- **UI**: Declarative Compose UI leveraging custom design tokens to match the web, eliminating generic Material 3 artifacts.
- **Data Layer**: Room Database for offline-first persistence. Implements a persistent offline pending operations queue (`PendingOpEntity`) to ensure data integrity during network reconnects.
- **Network**: Supabase Android SDK (Postgrest, Realtime, Auth), TMDB, and Jikan APIs (via Retrofit/Moshi).

### 3.2 Web
A lightweight, high-performance web experience.
- **UI**: HTML5, Vanilla JS, and Tailwind CSS. Employs stark contrast and bold typographical hierarchies.
- **Data Layer**: LocalStorage-backed synchronization queue (`loopa_sync_queue`) processing operations via a Last-Write-Wins (LWW) conflict resolution strategy via `updated_at`.
- **Network**: Supabase JS SDK, direct REST calls for TMDB and Jikan.

## 4. Current Objectives
The primary focus is the execution of the **Loopa Unification Implementation Plan**:
1. **Data Integrity (High Priority)**: Add `updated_at` timestamps to `media_items` and `watched_episodes` in Supabase to support Last-Write-Wins offline conflict resolution. Migrate Android's Room DB (v5→v6→v7) to support the `PendingOpDao`. Upgrade Realtime to sync changes symmetrically.
2. **Visual Polish**: Align Android design tokens perfectly with the Web master configuration. Remove obsolete fonts (`BebasNeue` references), and introduce genre chips and progress bars across both platforms.
3. **Feature Parity**: Unify AI Prompt Schema to standard `{title, mediaType, genre, releaseYear, reasoning}`. Support Jikan anime search on Android in parallel with TMDB. Implement Guest Mode for local-only Room data logging.
