import { getCorsHeaders } from '../utils/cors.js';
import { getCachedResponse, setCachedResponse } from '../utils/cache.js';
import { searchTmdb } from '../services/tmdb.js';
import { searchAniList } from '../services/anilist.js';
import { normalizeForSearch } from '../utils/normalizer.js';

export async function handleSemanticSearch(request, env, ctx) {
  const corsHeaders = getCorsHeaders(request);
  let query = '';

  if (request.method === 'GET') {
    const url = new URL(request.url);
    query = url.searchParams.get('q') || url.searchParams.get('query') || '';
  } else if (request.method === 'POST') {
    try {
      const body = await request.json();
      query = body.query || body.q || body.prompt || '';
    } catch {
      query = '';
    }
  }

  query = query.trim();
  if (!query) {
    return new Response(JSON.stringify([]), {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders }
    });
  }

  // 1. Check Edge Cache
  const cached = await getCachedResponse(request);
  if (cached) {
    const cachedData = await cached.text();
    return new Response(cachedData, {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders, 'X-Cache': 'HIT' }
    });
  }

  // 2. AI Reasoning Prompt
  const prompt = `
You are Loopa's AI Semantic Search Engine — an expert film, television, and anime curator with comprehensive encyclopedic knowledge.
The user is searching for media using a natural language query, theme, or concept:
Query: "${query}"

CRITICAL CURATION GUIDELINES:
1. Multi-facet Synthesis: If the query specifies multiple concepts (e.g. "robots and cars", "time travel romance", "space western"), prioritize acclaimed titles that embody BOTH/ALL concepts simultaneously (e.g. "Transformers", "Bumblebee", "Real Steel", "Knight Rider", "Speed Racer", "Megas XLR", "Redline") rather than splitting results across single keywords in isolation.
2. Acclaim & Popularity: Prioritize universally recognized, highly rated, culturally significant movies/shows/anime over obscure niche titles.
3. Media Type Alignment: If the user explicitly asks for "movies", "anime", or "tv shows", strictly provide that specific medium.
4. Exact Official Titles: Use the exact canonical English release title as indexed on TMDB/MyAnimeList.

Return 6 specific, top-tier recommendations.
Respond STRICTLY with a valid JSON array of objects without markdown fences:
[
  {
    "title": "Exact Official Title",
    "mediaType": "movie" or "tv" or "anime",
    "year": "YYYY",
    "reason": "1 concise sentence explaining exactly how it satisfies the user's query"
  }
]
`.trim();

  let aiSuggestions = [];

  // Attempt 1: Gemini
  if (env.GEMINI_API_KEY) {
    try {
      const geminiRes = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=${env.GEMINI_API_KEY}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: { temperature: 0.7, maxOutputTokens: 1024 }
        })
      });

      if (geminiRes.ok) {
        const data = await geminiRes.json();
        let text = data.candidates?.[0]?.content?.parts?.[0]?.text || '[]';
        text = text.replace(/```json?/g, '').replace(/```/g, '').trim();
        aiSuggestions = JSON.parse(text);
      }
    } catch (err) {
      console.warn('[Semantic Search] Gemini error:', err.message);
    }
  }

  // Attempt 2: Groq Fallback (GPT OSS 120B)
  if ((!aiSuggestions || aiSuggestions.length === 0) && env.GROQ_API_KEY) {
    try {
      const groqRes = await fetch('https://api.groq.com/openai/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${env.GROQ_API_KEY}`
        },
        body: JSON.stringify({
          model: 'openai/gpt-oss-120b',
          messages: [{ role: 'user', content: prompt }],
          temperature: 0.7
        })
      });

      if (groqRes.ok) {
        const data = await groqRes.json();
        let content = data.choices?.[0]?.message?.content || '[]';
        content = content.replace(/```json?/g, '').replace(/```/g, '').trim();
        const parsed = JSON.parse(content);
        aiSuggestions = Array.isArray(parsed) ? parsed : (parsed.recommendations || parsed.results || []);
      }
    } catch (err) {
      console.warn('[Semantic Search] Groq error:', err.message);
    }
  }

  if (!Array.isArray(aiSuggestions) || aiSuggestions.length === 0) {
    // Fallback if AI fails: perform normal search
    return new Response(JSON.stringify([]), {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders }
    });
  }

  // 3. Concurrently enrich each AI match with TMDB / AniList metadata & poster
  const enrichedResults = await Promise.all(
    aiSuggestions.map(async (item) => {
      try {
        const title = item.title;
        const type = (item.mediaType || 'movie').toLowerCase();

        if (type === 'anime') {
          const anilistList = await searchAniList(title);
          if (anilistList && anilistList.length > 0) {
            const norm = normalizeForSearch(anilistList[0], 'anilist');
            if (norm) {
              return {
                ...norm,
                isAiMatch: true,
                aiReason: item.reason || `Matches "${query}"`
              };
            }
          }
        }

        const tmdbList = await searchTmdb(title, env);
        if (tmdbList && tmdbList.length > 0) {
          const match = tmdbList.find(t => t.media_type === type) || tmdbList[0];
          const norm = normalizeForSearch(match, 'tmdb');
          if (norm) {
            return {
              ...norm,
              isAiMatch: true,
              aiReason: item.reason || `Matches "${query}"`
            };
          }
        }
      } catch (err) {
        console.warn(`[Semantic Search] Failed enriching ${item.title}:`, err);
      }
      return null;
    })
  );

  const validResults = enrichedResults.filter(Boolean);

  const response = new Response(JSON.stringify(validResults), {
    status: 200,
    headers: {
      'Content-Type': 'application/json',
      'X-Cache': 'MISS',
      'Cache-Control': 'public, max-age=86400, s-maxage=86400',
      ...corsHeaders
    }
  });

  if (ctx && validResults.length > 0) {
    await setCachedResponse(request, response, ctx, 86400);
  }

  return response;
}
