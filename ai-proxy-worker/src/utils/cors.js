const allowedOrigins = [
  'https://loopa.app',
  'https://loopa1.netlify.app',
  'https://dragonballsuper-1995.github.io',
  'http://localhost:5173',
  'http://localhost:3000',
  'http://127.0.0.1:5500',
  'http://localhost:5500'
];

export function getCorsHeaders(request, isPublic = false) {
  const origin = request.headers.get('Origin');
  const activeOrigin = isPublic ? '*' : (allowedOrigins.includes(origin) ? origin : 'https://loopa.app');

  return {
    'Access-Control-Allow-Origin': activeOrigin,
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Loopa-Client-Key',
  };
}

export function handleOptions(request) {
  return new Response(null, { headers: getCorsHeaders(request, true) });
}

export function authorizeClient(request, env) {
  const expectedKey = env.LOOPA_CLIENT_KEY;
  if (!expectedKey) return true; // If key is not configured in env, pass through

  const url = new URL(request.url);
  const paramKey = url.searchParams.get('k') || url.searchParams.get('key') || url.searchParams.get('client_key');
  const headerKey = request.headers.get('X-Loopa-Client-Key');

  return paramKey === expectedKey || headerKey === expectedKey;
}
