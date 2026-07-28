# System Architecture Blueprint

## 1. Tech Stack
- **Web App**: Vanilla HTML5, JavaScript (No bundler), Tailwind CSS. Served via Firebase Hosting.
- **Android App**: Kotlin, Jetpack Compose, Room (v6+), Coroutines/Flow, Retrofit/Moshi.
- **Backend/Database**: Supabase (PostgreSQL 15+, Auth, Realtime, REST API).
- **AI Proxy**: Cloudflare Worker (`loopa-ai-proxy...workers.dev`) interfacing with Google Gemini.

## 2. File & Folder Structure
```text
loopa/
├── app/                  # Android Native Project (Kotlin/Compose)
│   ├── src/main/java/com/loopa/
│   │   ├── db/           # Room entities, DAOs, Migrations (MediaItemEntity, PendingOpEntity, etc.)
│   │   ├── model/        # Remote DTOs (RemoteMediaItem, RemoteWatchedEpisode)
│   │   ├── network/      # API Services (TMDB, Jikan, Supabase Client setup)
│   │   ├── repository/   # Offline-first repositories handling DB & Sync
│   │   ├── ui/           # Jetpack Compose Screens and Theme Definitions
│   │   └── viewmodel/    # StateFlow management
├── website/              # Web Frontend (HTML/JS/CSS)
│   ├── index.html        # Main SPA interface and Tailwind Config
│   └── js/
│       ├── api.js        # TMDB/Jikan integrations
│       ├── app.js        # State, listeners, and view switching
│       ├── supabase.js   # Supabase DB operations and offline sync queue
│       └── ui.js         # DOM manipulation and component generation
├── docs/                 # Core documentation (You are here)
└── ai-proxy-worker/      # Cloudflare worker code
```

## 3. Data Flow & Offline Synchronization Strategy

The system relies on an **Offline-First, Last-Write-Wins (LWW)** architecture:
1. **Local Writes**: All user actions (ADD, UPDATE, DELETE, TOGGLE_EPISODE) immediately update local persistence.
   - **Android**: Inserts/Updates into Room DB.
   - **Web**: Updates active memory state / `drawerDBEntry`.
2. **Network Mutation**: The app attempts to push changes to Supabase via REST/Postgrest.
3. **Queueing (On Failure)**:
   - **Android**: Appends the mutation (e.g., `UPSERT_MEDIA`, `DELETE_EPISODE`) to `PendingOpEntity` in Room.
   - **Web**: Appends to `loopa_sync_queue` in LocalStorage.
4. **Reconnection Flush**: On network restoration (`ConnectivityManager` on Android, `window.ononline` on Web), the queue is flushed FIFO. 
5. **Conflict Resolution**: The app compares the queued operation's local timestamp against the server's `updated_at` column. Stale ops are discarded; newer ops are applied.
6. **Realtime**: Changes committed to Supabase broadcast `INSERT/UPDATE/DELETE` actions via Supabase Realtime back to clients to keep connected devices in sync without full polling.

## 4. AI Recommendation Pipeline
1. Client constructs context (watched lists, current interactions, watch history).
2. Request dispatched to Cloudflare Worker.
3. Cloudflare Worker proxies securely to Google Gemini APIs.
4. Returns strictly formatted JSON: `{"title": "...", "mediaType": "movie|tv|anime", "genre": "...", "releaseYear": "...", "reasoning": "..."}`.
5. Client orchestrates background parallel searches across TMDB and Jikan to hydrate posters and IDs for the recommendations.
