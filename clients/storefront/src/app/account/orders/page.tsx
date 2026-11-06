import Link from 'next/link';
import type { Metadata } from 'next';
import { accessToken, apiUrl } from '@/lib/auth';
import { formatPrice } from '@/lib/api';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Мои заказы — Plant Market',
  robots: { index: false, follow: false },
};

interface OrderSummary {
  id: string;
  status: string;
  totalMinor: number;
  currency: string;
  createdAt: string;
  items: { name: string; quantity: number }[];
}

export default async function OrdersPage() {
  const token = await accessToken();
  if (!token) {
    return <LoginPrompt />;
  }
  const response = await fetch(`${apiUrl()}/api/v1/orders`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: 'no-store',
  });
  if (response.status === 401) {
    return <LoginPrompt />;
  }
  if (!response.ok) {
    return <p className="error">Не удалось загрузить заказы (HTTP {response.status}).</p>;
  }
  const orders = (await response.json()) as OrderSummary[];
  if (orders.length === 0) {
    return (
      <section>
        <h1>Мои заказы</h1>
        <p className="empty">
          Заказов пока нет. <Link href="/catalog">Перейти в каталог</Link>.
        </p>
      </section>
    );
  }
  return (
    <section>
      <h1>Мои заказы</h1>
      <ul className="order-list">
        {orders.map((order) => (
          <li key={order.id} className="order-card">
            <div>
              <p className="sku">{order.id.slice(-8)}</p>
              <h2>
                <Link href={`/account/orders/${order.id}`}>
                  {order.items.map((item) => item.name).join(', ')}
                </Link>
              </h2>
              <p className="muted">
                {new Date(order.createdAt).toLocaleDateString('be-BY')} · {order.status}
              </p>
            </div>
            <p className="price">{formatPrice(order.totalMinor, order.currency)}</p>
          </li>
        ))}
      </ul>
    </section>
  );
}

function LoginPrompt() {
  return (
    <section className="checkout-result" role="status">
      <h1>Войдите, чтобы увидеть заказы</h1>
      <a className="button-link" href="/api/auth/login?returnTo=/account/orders">
        Войти
      </a>
    </section>
  );
}
