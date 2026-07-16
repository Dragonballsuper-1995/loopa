export default {
  async fetch(request, env, ctx) {
    // Add CORS headers so web and android apps can fetch this API
    const allowedOrigins = [
      'https://loopa.app',
      'http://localhost:5173',
      'http://localhost:3000',
      'http://127.0.0.1:5500',
      'http://localhost:5500'
    ];
    const url = new URL(request.url);
    const isTmdbImage = url.pathname.startsWith('/tmdb/t/p/');
    const origin = request.headers.get('Origin');
    const activeOrigin = isTmdbImage ? '*' : (allowedOrigins.includes(origin) ? origin : 'https://loopa.app');

    const corsHeaders = {
      'Access-Control-Allow-Origin': activeOrigin,
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Loopa-Client-Key',
    };

    // Handle OPTIONS requests for CORS preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    // For all non-OPTIONS requests, validate client authorization key (bypass for public TMDB images)
    if (!isTmdbImage) {
      const clientKey = request.headers.get('X-Loopa-Client-Key');
      if (clientKey !== env.LOOPA_CLIENT_KEY) {
        return new Response(JSON.stringify({ error: 'Unauthorized Client Request' }), {
          status: 403,
          headers: { 'Content-Type': 'application/json', ...corsHeaders },
        });
      }
    }

    // --- Proxy TMDB Requests Securely ---
    if (url.pathname.startsWith('/tmdb/')) {
      if (request.method !== 'GET') {
        return new Response(JSON.stringify({ error: 'Method Not Allowed' }), {
          status: 405,
          headers: { 'Content-Type': 'application/json', ...corsHeaders },
        });
      }

      const subpath = url.pathname.replace(/^\/tmdb\//, '');
      const isImage = subpath.startsWith('t/p/');
      const targetHost = isImage ? 'https://image.tmdb.org/' : 'https://api.themoviedb.org/';
      const targetUrl = new URL(targetHost + subpath);
      
      // Copy search parameters
      url.searchParams.forEach((value, key) => {
        targetUrl.searchParams.set(key, value);
      });
      
      // Inject TMDB API Key from environment secrets (images don't require an API key)
      if (!isImage) {
        targetUrl.searchParams.set('api_key', env.TMDB_API_KEY || env.TMDB_KEY);
      }

      try {
        const headers = {};
        if (!isImage) {
          headers['Accept'] = 'application/json';
        }
        const tmdbRes = await fetch(targetUrl.toString(), {
          method: 'GET',
          headers: headers
        });

        const data = await tmdbRes.arrayBuffer();
        return new Response(data, {
          status: tmdbRes.status,
          headers: {
            'Content-Type': tmdbRes.headers.get('Content-Type') || (isImage ? 'image/jpeg' : 'application/json'),
            ...corsHeaders
          }
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: 'Failed to proxy TMDB request', details: err.message }), {
          status: 502,
          headers: { 'Content-Type': 'application/json', ...corsHeaders }
        });
      }
    }

    if (request.method !== 'POST') {
      return new Response(JSON.stringify({ error: 'Method Not Allowed' }), {
        status: 405,
        headers: { 'Content-Type': 'application/json', ...corsHeaders },
      });
    }

    try {
      const { prompt } = await request.json();

      if (!prompt) {
        return new Response(JSON.stringify({ error: 'Missing prompt in request body' }), {
          status: 400,
          headers: { 'Content-Type': 'application/json', ...corsHeaders },
        });
      }

      // --- Attempt 1: Gemini ---
      try {
        const geminiRes = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=${env.GEMINI_API_KEY}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: { temperature: 0.75, maxOutputTokens: 1024 },
          }),
        });

        if (geminiRes.ok) {
          const data = await geminiRes.json();
          let text = data.candidates?.[0]?.content?.parts?.[0]?.text || '[]';
          text = text.replace(/```json?/g, '').replace(/```/g, '').trim();
          return new Response(text, {
            status: 200,
            headers: { 'Content-Type': 'application/json', ...corsHeaders },
          });
        } else {
          console.error(`Gemini failed: ${geminiRes.status}`);
        }
      } catch (err) {
        console.error('Gemini error:', err);
      }

      // --- Attempt 2: Groq ---
      try {
        const groqRes = await fetch('https://api.groq.com/openai/v1/chat/completions', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${env.GROQ_API_KEY}`,
          },
          body: JSON.stringify({
            model: 'llama-3.3-70b-versatile',
            messages: [{ role: 'user', content: prompt }],
            response_format: { type: 'json_object' },
          }),
        });

        if (groqRes.ok) {
          const data = await groqRes.json();
          let content = data.choices?.[0]?.message?.content || '[]';
          content = content.replace(/```json?/g, '').replace(/```/g, '').trim();
          return new Response(content, {
            status: 200,
            headers: { 'Content-Type': 'application/json', ...corsHeaders },
          });
        } else {
          console.error(`Groq failed: ${groqRes.status}`);
        }
      } catch (err) {
        console.error('Groq error:', err);
      }

      // --- Attempt 3: OpenRouter ---
      try {
        const orRes = await fetch('https://openrouter.ai/api/v1/chat/completions', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${env.OPENROUTER_API_KEY}`,
          },
          body: JSON.stringify({
            model: 'google/gemma-4-31b-a4b:free',
            messages: [{ role: 'user', content: prompt }],
          }),
        });

        if (orRes.ok) {
          const data = await orRes.json();
          let content = data.choices?.[0]?.message?.content || '[]';
          content = content.replace(/```json?/g, '').replace(/```/g, '').trim();
          return new Response(content, {
            status: 200,
            headers: { 'Content-Type': 'application/json', ...corsHeaders },
          });
        } else {
          console.error(`OpenRouter failed: ${orRes.status}`);
        }
      } catch (err) {
        console.error('OpenRouter error:', err);
      }

      return new Response(JSON.stringify({ error: 'All AI providers exhausted.' }), {
        status: 503,
        headers: { 'Content-Type': 'application/json', ...corsHeaders },
      });

    } catch (error) {
      return new Response(JSON.stringify({ error: 'Internal Server Error', details: error.message }), {
        status: 500,
        headers: { 'Content-Type': 'application/json', ...corsHeaders },
      });
    }
  },
};
