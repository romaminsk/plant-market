import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { ACCESS_COOKIE, apiUrl } from '@/lib/auth';

export async function POST(request: NextRequest) {
  const token = request.cookies.get(ACCESS_COOKIE)?.value;
  if (!token) {
    return NextResponse.json({ detail: 'Authentication required' }, { status: 401 });
  }
  const csrfCookie = request.cookies.get('pm.csrf')?.value;
  const csrfHeader = request.headers.get('x-csrf-token');
  if (!csrfCookie || csrfHeader !== csrfCookie) {
    return NextResponse.json({ detail: 'CSRF token mismatch' }, { status: 403 });
  }
  const payload = (await request.json()) as {
    plantId?: string;
    shippingZoneId?: string;
    thermalPack?: boolean;
  };
  if (!payload.plantId) {
    return NextResponse.json({ detail: 'plantId is required' }, { status: 400 });
  }
  const headers = {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
  const base = apiUrl();

  const orderResponse = await fetch(`${base}/api/v1/orders`, {
    method: 'POST',
    headers,
    cache: 'no-store',
    body: JSON.stringify({
      items: [{ individualPlantId: payload.plantId, quantity: 1 }],
    }),
  });
  if (!orderResponse.ok) {
    return forward(orderResponse);
  }
  const order = (await orderResponse.json()) as { id: string };

  const checkoutResponse = await fetch(`${base}/api/v1/orders/${order.id}/checkout`, {
    method: 'POST',
    headers,
    cache: 'no-store',
    body: JSON.stringify({
      shippingZoneId: payload.shippingZoneId ?? null,
      thermalPack: Boolean(payload.thermalPack),
    }),
  });
  if (!checkoutResponse.ok) {
    return forward(checkoutResponse);
  }

  const payResponse = await fetch(`${base}/api/v1/orders/${order.id}/pay`, {
    method: 'POST',
    headers,
    cache: 'no-store',
    body: '{}',
  });
  if (!payResponse.ok) {
    return forward(payResponse);
  }
  const paid = (await payResponse.json()) as { id: string; status: string };
  return NextResponse.json({ orderId: paid.id, status: paid.status });
}

async function forward(response: Response) {
  const problem = (await response.json().catch(() => null)) as { detail?: string } | null;
  return NextResponse.json(
    { detail: problem?.detail ?? `Request failed (${response.status})` },
    { status: response.status },
  );
}
