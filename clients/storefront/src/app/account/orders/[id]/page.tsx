import Link from 'next/link';
import type { Metadata } from 'next';
import { accessToken, apiUrl } from '@/lib/auth';
import { formatPrice } from '@/lib/api';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Заказ — Plant Market',
  robots: { index: false, follow: false },
};

interface OrderDetail {
  id: string;
  status: string;
  totalMinor: number;
  shippingMinor: number;
  currency: string;
  createdAt: string;
  shippingZoneId: string | null;
  thermalPack: boolean;
  items: { id: string; name: string; quantity: number; lineTotalMinor: number }[];
}

export default async function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const token = await accessToken();
  if (!token) {
    return <LoginPrompt returnTo={`/account/orders/${id}`} />;
  }
  const response = await fetch(`${apiUrl()}/api/v1/orders/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: 'no-store',
  });
  if (response.status === 401) {
    return <LoginPrompt returnTo={`/account/orders/${id}`} />;
  }
  if (!response.ok) {
    return <p className="error">Заказ не найден (HTTP {response.status}).</p>;
  }
  const order = (await response.json()) as OrderDetail;
  return (
    <section>
      <Link className="back-link" href="/account/orders">
        ← Мои заказы
      </Link>
      <h1>Заказ {order.id.slice(-8)}</h1>
      <p className="muted">
        {new Date(order.createdAt).toLocaleString('be-BY')} · {order.status}
      </p>
      <ul className="cart-list">
        {order.items.map((item) => (
          <li key={item.id} className="cart-item">
            <span>{item.name}</span>
            <span>× {item.quantity}</span>
            <span>{formatPrice(item.lineTotalMinor, order.currency)}</span>
          </li>
        ))}
      </ul>
      <dl className="checkout-total">
        <div>
          <dt>Доставка</dt>
          <dd>{formatPrice(order.shippingMinor, order.currency)}</dd>
        </div>
        <div>
          <dt>Итого</dt>
          <dd>{formatPrice(order.totalMinor, order.currency)}</dd>
        </div>
      </dl>
      {order.thermalPack ? <p className="muted">Термоупаковка включена.</p> : null}
    </section>
  );
}

function LoginPrompt({ returnTo }: { returnTo: string }) {
  return (
    <section className="checkout-result" role="status">
      <h1>Войдите, чтобы увидеть заказ</h1>
      <a className="button-link" href={`/api/auth/login?returnTo=${encodeURIComponent(returnTo)}`}>
        Войти
      </a>
    </section>
  );
}
