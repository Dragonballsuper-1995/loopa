export default {
  async fetch(request, env, ctx) {
    // Add CORS headers so web and android apps can fetch this API
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    };

    // Handle OPTIONS requests for CORS preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
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
