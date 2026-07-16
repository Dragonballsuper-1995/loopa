/**
 * Miru Web — Global Configuration
 * API keys & base URLs from project .env
 */
const CONFIG = {
    // Client security key for proxy validation
    CLIENT_KEY:    'loopa_secure_client_auth_secret_2026_x',

    // TMDB (Proxied through Cloudflare Worker)
    TMDB_BASE:     'https://loopa-ai-proxy.sujalsanjay-chhajed2023.workers.dev/tmdb/3',
    TMDB_IMG_500:  'https://loopa-ai-proxy.sujalsanjay-chhajed2023.workers.dev/tmdb/t/p/w500',
    TMDB_IMG_780:  'https://loopa-ai-proxy.sujalsanjay-chhajed2023.workers.dev/tmdb/t/p/w780',
    TMDB_IMG_ORIG: 'https://loopa-ai-proxy.sujalsanjay-chhajed2023.workers.dev/tmdb/t/p/original',

    // Jikan (MyAnimeList) — no key required
    JIKAN_BASE: 'https://api.jikan.moe/v4',

    // Supabase
    SUPABASE_URL: 'https://utkqrfdheofuepeuzutg.supabase.co',
    SUPABASE_KEY: 'sb_publishable_OFJE4wCeLAUAw3ufKHlA1Q_4oE4Ap5g',

    // AI Proxy (Cloudflare Worker)
    AI_PROXY_URL: 'https://loopa-ai-proxy.sujalsanjay-chhajed2023.workers.dev', // e.g., 'https://loopa-ai-proxy.your-username.workers.dev'


    // Database table — same table used by the Android app
    DB_TABLE: 'media_items',
};
