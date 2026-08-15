const staticAllowedOrigins = [
  'https://loopa.app',
  'https://loopa1.netlify.app',
  'https://dragonballsuper-1995.github.io',
  'http://localhost:8899',
  'http://127.0.0.1:8899',
  'http://localhost:5500',
  'http://127.0.0.1:5500',
  'http://localhost:3000',
  'http://localhost:5173'
];

export function isAllowedOrigin(origin) {
  if (!origin) return false;
  if (staticAllowedOrigins.includes(origin)) return true;
  // Match any localhost or 127.0.0.1 on any port (for local dev servers)
  if (/^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(origin)) return true;
  // Match Netlify or Cloudflare Pages preview domains
  if (/^https:\/\/[a-z0-9-]+\.netlify\.app$/.test(origin)) return true;
  if (/^https:\/\/[a-z0-9-]+\.pages\.dev$/.test(origin)) return true;
  return false;
}

export function getCorsHeaders(request, isPublic = false) {
  let activeOrigin = 'https://loopa.app';
  if (request) {
    const origin = request.headers.get('Origin');
    if (isPublic) {
      activeOrigin = origin || '*';
    } else if (origin && isAllowedOrigin(origin)) {
      activeOrigin = origin;
    }
  }

  return {
    'Access-Control-Allow-Origin': activeOrigin,
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Loopa-Client-Key',
    'Access-Control-Max-Age': '86400',
  };
}

export function handleOptions(request) {
  return new Response(null, {
    status: 204,
    headers: getCorsHeaders(request, true)
  });
}

export function authorizeClient(request, env) {
  const expectedKey = env.LOOPA_CLIENT_KEY;
  if (!expectedKey) return true; // If key is not configured in env, pass through

  const url = new URL(request.url);
  const paramKey = url.searchParams.get('k') || url.searchParams.get('key') || url.searchParams.get('client_key');
  const headerKey = request.headers.get('X-Loopa-Client-Key');

  return paramKey === expectedKey || headerKey === expectedKey;
}
