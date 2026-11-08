import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { ACCESS_COOKIE, COOKIE_SECURE, REFRESH_COOKIE, authServiceUrl } from '@/lib/auth';
import { safeReturnTo } from '@/lib/safe-redirect';

export async function GET(request: NextRequest) {
  const code = request.nextUrl.searchParams.get('code');
  const state = request.nextUrl.searchParams.get('state');
  const verifier = request.cookies.get('pm.pkce')?.value;
  const expectedState = request.cookies.get('pm.state')?.value;
  const returnTo = safeReturnTo(request.cookies.get('pm.return')?.value);
  const origin = request.nextUrl.origin;

  if (!code || !verifier || !state || state !== expectedState) {
    return NextResponse.redirect(new URL(`${returnTo}?error=oauth`, request.url));
  }

  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: 'storefront',
    code,
    redirect_uri: `${origin}/auth/callback`,
    code_verifier: verifier,
  });
  const tokenResponse = await fetch(`${authServiceUrl()}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
    cache: 'no-store',
  });
  if (!tokenResponse.ok) {
    return NextResponse.redirect(new URL(`${returnTo}?error=token`, request.url));
  }
  const tokens = (await tokenResponse.json()) as {
    access_token: string;
    refresh_token?: string;
    expires_in?: number;
  };

  const secure = COOKIE_SECURE;
  const response = NextResponse.redirect(new URL(returnTo, request.url));
  response.cookies.set(ACCESS_COOKIE, tokens.access_token, {
    httpOnly: true,
    sameSite: 'lax',
    path: '/',
    maxAge: tokens.expires_in ?? 600,
    secure,
  });
  if (tokens.refresh_token) {
    response.cookies.set(REFRESH_COOKIE, tokens.refresh_token, {
      httpOnly: true,
      sameSite: 'lax',
      path: '/',
      maxAge: 30 * 24 * 3600,
      secure,
    });
  }
  response.cookies.set('pm.csrf', crypto.randomUUID(), {
    httpOnly: false,
    sameSite: 'lax',
    path: '/',
    maxAge: 30 * 24 * 3600,
    secure,
  });
  response.cookies.delete('pm.pkce');
  response.cookies.delete('pm.state');
  response.cookies.delete('pm.return');
  return response;
}
