import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { COOKIE_SECURE, authServiceUrl } from '@/lib/auth';
import { createChallenge, createVerifier } from '@/lib/pkce';

const COOKIE_OPTIONS = {
  httpOnly: true,
  sameSite: 'lax' as const,
  path: '/',
  maxAge: 600,
  secure: COOKIE_SECURE,
};

export function GET(request: NextRequest) {
  const origin = request.nextUrl.origin;
  const verifier = createVerifier();
  const state = createVerifier();
  const requested = request.nextUrl.searchParams.get('returnTo') ?? '/checkout';
  const returnTo =
    requested.startsWith('/') && !requested.startsWith('//') ? requested : '/checkout';

  const authorize = new URL('/oauth2/authorize', authServiceUrl());
  authorize.searchParams.set('response_type', 'code');
  authorize.searchParams.set('client_id', 'storefront');
  authorize.searchParams.set('redirect_uri', `${origin}/auth/callback`);
  authorize.searchParams.set('scope', 'catalog.read');
  authorize.searchParams.set('code_challenge', createChallenge(verifier));
  authorize.searchParams.set('code_challenge_method', 'S256');
  authorize.searchParams.set('state', state);

  const response = NextResponse.redirect(authorize);
  response.cookies.set('pm.pkce', verifier, COOKIE_OPTIONS);
  response.cookies.set('pm.state', state, COOKIE_OPTIONS);
  response.cookies.set('pm.return', returnTo, COOKIE_OPTIONS);
  return response;
}
