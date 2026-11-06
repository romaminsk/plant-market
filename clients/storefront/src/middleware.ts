import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/** Guest checkout needs a CSRF token even before the OAuth2 callback sets one. */
export function middleware(request: NextRequest) {
  const response = NextResponse.next();
  if (!request.cookies.get('pm.csrf')) {
    response.cookies.set('pm.csrf', crypto.randomUUID(), {
      httpOnly: false,
      sameSite: 'lax',
      path: '/',
      secure: process.env.COOKIE_SECURE === 'true',
    });
  }
  return response;
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|images|fonts|favicon.svg|robots.txt|sitemap.xml).*)'],
};
