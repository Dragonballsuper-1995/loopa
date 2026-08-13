import { getCorsHeaders } from '../utils/cors.js';

export async function handleAiRecommendations(request, env) {
  const corsHeaders = getCorsHeaders(request);

  if (request.method !== 'POST') {
    return new Response(JSON.stringify({ error: 'Method Not Allowed' }), {
      status: 405,
      headers: { 'Content-Type': 'application/json', ...corsHeaders }
    });
  }

  try {
    const body = await request.json();
    let prompt = body.prompt;

    // If structured prompt construction is passed from client
    if (!prompt && body.history) {
      const isColdStart = !body.history || body.history.length < 3;
      const historyListString = isColdStart ? '' : body.history.map(item => `- [${item.mediaType}] ${item.title}`).join('\n');
      const likedString = body.likedTitles && body.likedTitles.length > 0
        ? `\nThe user LIKED:\n` + body.likedTitles.map(t => `- ${t}`).join('\n') : '';
      const dislikedString = body.dislikedTitles && body.dislikedTitles.length > 0
        ? `\nThe user DISLIKED:\n` + body.dislikedTitles.map(t => `- ${t}`).join('\n') : '';

      prompt = `
You are a conversational AI Recommendation Engine for media (Movies, TV Shows, Anime).
${isColdStart ? "The user is new." : "User watched media history:\n" + historyListString}
${likedString}
${dislikedString}

Provide exactly 4 recommendations. Respond STRICTLY with a valid JSON array of objects:
[{"title": "Title", "mediaType": "Movie/TV/Anime", "genre": "Genre", "releaseYear": "YYYY", "imageUrl": "Valid Poster Image URL", "reasoning": "Reasoning"}]
      `.trim();
    }

    if (!prompt) {
      return new Response(JSON.stringify({ error: 'Missing prompt in request body' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json', ...corsHeaders }
      });
    }

    // ── Attempt 1: Gemini ────────────────────────────────────────────────────
    if (env.GEMINI_API_KEY) {
      try {
        const geminiRes = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=${env.GEMINI_API_KEY}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: { temperature: 0.75, maxOutputTokens: 1024 }
          })
        });

        if (geminiRes.ok) {
          const data = await geminiRes.json();
          let text = data.candidates?.[0]?.content?.parts?.[0]?.text || '[]';
          text = text.replace(/```json?/g, '').replace(/```/g, '').trim();
          return new Response(text, {
            status: 200,
            headers: { 'Content-Type': 'application/json', ...corsHeaders }
          });
        }
      } catch (err) {
        console.error('[AI Service] Gemini error:', err.message);
      }
    }

    // ── Attempt 2: Groq ──────────────────────────────────────────────────────
    if (env.GROQ_API_KEY) {
      try {
        const groqRes = await fetch('https://api.groq.com/openai/v1/chat/completions', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${env.GROQ_API_KEY}`
          },
          body: JSON.stringify({
            model: 'llama-3.3-70b-versatile',
            messages: [{ role: 'user', content: prompt }],
            response_format: { type: 'json_object' }
          })
        });

        if (groqRes.ok) {
          const data = await groqRes.json();
          let content = data.choices?.[0]?.message?.content || '[]';
          content = content.replace(/```json?/g, '').replace(/```/g, '').trim();
          return new Response(content, {
            status: 200,
            headers: { 'Content-Type': 'application/json', ...corsHeaders }
          });
        }
      } catch (err) {
        console.error('[AI Service] Groq error:', err.message);
      }
    }

    // ── Attempt 3: OpenRouter ────────────────────────────────────────────────
    if (env.OPENROUTER_API_KEY) {
      try {
        const orRes = await fetch('https://openrouter.ai/api/v1/chat/completions', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${env.OPENROUTER_API_KEY}`
          },
          body: JSON.stringify({
            model: 'google/gemma-4-31b-a4b:free',
            messages: [{ role: 'user', content: prompt }]
          })
        });

        if (orRes.ok) {
          const data = await orRes.json();
          let content = data.choices?.[0]?.message?.content || '[]';
          content = content.replace(/```json?/g, '').replace(/```/g, '').trim();
          return new Response(content, {
            status: 200,
            headers: { 'Content-Type': 'application/json', ...corsHeaders }
          });
        }
      } catch (err) {
        console.error('[AI Service] OpenRouter error:', err.message);
      }
    }

    return new Response(JSON.stringify({ error: 'All AI providers exhausted.' }), {
      status: 503,
      headers: { 'Content-Type': 'application/json', ...corsHeaders }
    });

  } catch (error) {
    return new Response(JSON.stringify({ error: 'Internal Server Error', details: error.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json', ...corsHeaders }
    });
  }
}
