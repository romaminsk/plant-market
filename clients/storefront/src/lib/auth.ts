import { cookies } from 'next/headers';

const secure = process.env.COOKIE_SECURE === 'true';
const prefix = secure ? '__Host-' : '';

export const COOKIE_SECURE = secure;
export const ACCESS_COOKIE = `${prefix}pm.access`;
export const REFRESH_COOKIE = `${prefix}pm.refresh`;

export function authServiceUrl(): string {
  return process.env.AUTH_SERVICE_URL ?? 'http://localhost:8080';
}

export function apiUrl(): string {
  return process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
}

export async function accessToken(): Promise<string | null> {
  const store = await cookies();
  return store.get(ACCESS_COOKIE)?.value ?? null;
}
