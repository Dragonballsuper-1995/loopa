/**
 * Miru Web — Global Configuration
 * API keys & base URLs from project .env
 */
const CONFIG = {
    // TMDB
    TMDB_KEY:      'd639fb83cba55b0d87df18a1e80ad88f',
    TMDB_BASE:     'https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/3',
    TMDB_IMG_500:  'https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w500',
    TMDB_IMG_780:  'https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w780',
    TMDB_IMG_ORIG: 'https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/original',

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
