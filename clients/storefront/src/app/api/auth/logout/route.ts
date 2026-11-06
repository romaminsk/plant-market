import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { ACCESS_COOKIE, REFRESH_COOKIE, apiUrl } from '@/lib/auth';

export async function POST(request: NextRequest) {
  const token = request.cookies.get(ACCESS_COOKIE)?.value;
  if (token) {
    try {
      await fetch(`${apiUrl()}/api/v1/auth/logout`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        cache: 'no-store',
      });
    } catch {
      // Cookie cleanup still logs the browser out; refresh tokens expire by TTL.
    }
  }
  const response = NextResponse.redirect(new URL('/', request.url));
  response.cookies.delete(ACCESS_COOKIE);
  response.cookies.delete(REFRESH_COOKIE);
  return response;
}
