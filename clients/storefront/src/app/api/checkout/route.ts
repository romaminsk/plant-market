import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { ACCESS_COOKIE, REFRESH_COOKIE, apiUrl, authServiceUrl } from '@/lib/auth';

interface RefreshedTokens {
  access: string;
  refresh?: string;
}

interface CheckoutItem {
  type?: string;
  id?: string;
  quantity?: number;
}

const secure = process.env.COOKIE_SECURE === 'true';

export async function POST(request: NextRequest) {
  let token = request.cookies.get(ACCESS_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ detail: 'Authentication required' }, { status: 401 });
  }
  const csrfCookie = request.cookies.get('pm.csrf')?.value;
  const csrfHeader = request.headers.get('x-csrf-token');
  if (!csrfCookie || csrfHeader !== csrfCookie) {
    return NextResponse.json({ detail: 'CSRF token mismatch' }, { status: 403 });
  }

  const payload = (await request.json()) as {
    items?: CheckoutItem[];
    shippingZoneId?: string;
    thermalPack?: boolean;
  };
  const items = Array.isArray(payload.items) ? payload.items : [];
  if (items.length === 0 || items.length > 20) {
    return NextResponse.json({ detail: 'Cart must contain 1 to 20 items' }, { status: 400 });
  }
  const orderItems = [];
  for (const item of items) {
    if (!item.id || (item.type !== 'VARIANT' && item.type !== 'INDIVIDUAL_PLANT')) {
      return NextResponse.json({ detail: 'Invalid cart item' }, { status: 400 });
    }
    const quantity = Math.max(1, Math.min(10, item.quantity ?? 1));
    orderItems.push(
      item.type === 'VARIANT'
        ? { variantId: item.id, quantity }
        : { individualPlantId: item.id, quantity },
    );
  }

  const base = apiUrl();
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
  let refreshed: RefreshedTokens | null = null;

  async function apiFetch(path: string, init: RequestInit): Promise<Response> {
    const send = () =>
      fetch(`${base}${path}`, {
        ...init,
        cache: 'no-store',
        headers: {
          ...(init.headers ?? {}),
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });
    let response = await send();
    if (response.status === 401 && refreshToken && !refreshed) {
      refreshed = await refresh(refreshToken);
      if (refreshed) {
        token = refreshed.access;
        response = await send();
      }
    }
    return response;
  }

  function withTokens(response: NextResponse): NextResponse {
    if (refreshed) {
      response.cookies.set(ACCESS_COOKIE, refreshed.access, {
        httpOnly: true,
        sameSite: 'lax',
        path: '/',
        maxAge: 600,
        secure,
      });
      if (refreshed.refresh) {
        response.cookies.set(REFRESH_COOKIE, refreshed.refresh, {
          httpOnly: true,
          sameSite: 'lax',
          path: '/',
          maxAge: 30 * 24 * 3600,
          secure,
        });
      }
    }
    return response;
  }

  const orderResponse = await apiFetch('/api/v1/orders', {
    method: 'POST',
    body: JSON.stringify({ items: orderItems }),
  });
  if (!orderResponse.ok) {
    return withTokens(await forward(orderResponse));
  }
  const order = (await orderResponse.json()) as { id: string };

  const checkoutResponse = await apiFetch(`/api/v1/orders/${order.id}/checkout`, {
    method: 'POST',
    body: JSON.stringify({
      shippingZoneId: payload.shippingZoneId ?? null,
      thermalPack: Boolean(payload.thermalPack),
    }),
  });
  if (!checkoutResponse.ok) {
    return withTokens(await forward(checkoutResponse));
  }

  const payResponse = await apiFetch(`/api/v1/orders/${order.id}/pay`, {
    method: 'POST',
    body: '{}',
  });
  if (!payResponse.ok) {
    return withTokens(await forward(payResponse));
  }
  const paid = (await payResponse.json()) as { id: string; status: string };
  return withTokens(NextResponse.json({ orderId: paid.id, status: paid.status }));
}

async function refresh(refreshToken: string): Promise<RefreshedTokens | null> {
  const body = new URLSearchParams({
    grant_type: 'refresh_token',
    client_id: 'storefront',
    refresh_token: refreshToken,
  });
  const response = await fetch(`${authServiceUrl()}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
    cache: 'no-store',
  });
  if (!response.ok) {
    return null;
  }
  const tokens = (await response.json()) as { access_token: string; refresh_token?: string };
  return { access: tokens.access_token, refresh: tokens.refresh_token };
}

async function forward(response: Response): Promise<NextResponse> {
  const problem = (await response.json().catch(() => null)) as { detail?: string } | null;
  return NextResponse.json(
    { detail: problem?.detail ?? `Request failed (${response.status})` },
    { status: response.status },
  );
}
