import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { ACCESS_COOKIE } from '@/lib/auth';

export async function GET() {
  const store = await cookies();
  return NextResponse.json({ authenticated: Boolean(store.get(ACCESS_COOKIE)?.value) });
}
