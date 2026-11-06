'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { fetchShippingZones, formatPrice } from '@/lib/api';
import type { ShippingZone } from '@/lib/api';
import { clearCart, readCart } from '@/lib/cart';
import type { CartItem } from '@/lib/cart';

export default function CheckoutPage() {
  const [items, setItems] = useState<CartItem[]>([]);
  const [zones, setZones] = useState<ShippingZone[]>([]);
  const [zoneId, setZoneId] = useState('');
  const [thermal, setThermal] = useState(false);
  const [authenticated, setAuthenticated] = useState<boolean | null>(null);
  const [status, setStatus] = useState<'idle' | 'sending' | 'done' | 'error'>('idle');
  const [message, setMessage] = useState('');
  const [orderId, setOrderId] = useState('');

  useEffect(() => {
    setItems(readCart());
    fetchShippingZones()
      .then((loaded) => {
        setZones(loaded);
        if (loaded.length > 0) {
          setZoneId(loaded[0].id);
        }
      })
      .catch((cause: unknown) =>
        setMessage(cause instanceof Error ? cause.message : 'Не удалось загрузить зоны доставки'),
      );
    fetch('/api/session')
      .then((response) => response.json())
      .then((payload: { authenticated: boolean }) => setAuthenticated(payload.authenticated))
      .catch(() => setAuthenticated(false));
  }, []);

  const subtotal = items.reduce((total, item) => total + item.priceMinor * item.quantity, 0);
  const zone = zones.find((item) => item.id === zoneId);
  const shipping = zone
    ? (subtotal >= zone.freeFromMinor ? 0 : zone.priceMinor) + (thermal ? zone.thermalPackMinor : 0)
    : 0;

  async function submit() {
    setStatus('sending');
    setMessage('');
    try {
      const csrf =
        document.cookie
          .split('; ')
          .find((item) => item.startsWith('pm.csrf='))
          ?.split('=')[1] ?? '';
      const response = await fetch('/api/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf },
        body: JSON.stringify({
          items: items.map((item) => ({
            type: item.type,
            id: item.id,
            quantity: item.quantity,
          })),
          shippingZoneId: zoneId,
          thermalPack: thermal,
        }),
      });
      const payload = (await response.json().catch(() => null)) as {
        detail?: string;
        orderId?: string;
        status?: string;
      } | null;
      if (response.status === 401) {
        setAuthenticated(false);
        setStatus('idle');
        return;
      }
      if (!response.ok) {
        throw new Error(payload?.detail ?? `Оплата не прошла (HTTP ${response.status})`);
      }
      clearCart();
      setOrderId(payload?.orderId ?? '');
      setStatus('done');
    } catch (cause) {
      setStatus('error');
      setMessage(cause instanceof Error ? cause.message : 'Неизвестная ошибка');
    }
  }

  if (items.length === 0 && status !== 'done') {
    return (
      <section>
        <h1>Оформление заказа</h1>
        <p className="empty">
          Корзина пуста. <Link href="/catalog">Перейти в каталог</Link>.
        </p>
      </section>
    );
  }

  if (status === 'done') {
    return (
      <section className="checkout-result" role="status">
        <h1>Заказ оплачен</h1>
        <p>
          Номер заказа: <span className="mono">{orderId}</span>
        </p>
        <p className="muted">Подтверждение отправлено на e-mail. Спасибо!</p>
        <p>
          <Link href="/account/orders">Мои заказы</Link>
        </p>
      </section>
    );
  }

  if (authenticated === false) {
    return (
      <section className="checkout-result" role="status">
        <h1>Нужно войти</h1>
        <p className="muted">Войдите, чтобы оформить и оплатить заказ.</p>
        <a className="button-link" href="/api/auth/login?returnTo=/checkout">
          Войти
        </a>
      </section>
    );
  }

  return (
    <section>
      <h1>Оформление заказа</h1>
      <div className="checkout-layout">
        <div className="checkout-summary">
          <h2>Состав заказа</h2>
          <ul className="cart-list compact">
            {items.map((item) => (
              <li key={`${item.type}-${item.id}`} className="cart-item">
                <span>{item.name}</span>
                <span>{formatPrice(item.priceMinor * item.quantity, item.currency)}</span>
              </li>
            ))}
          </ul>
        </div>
        <div className="checkout-form">
          <label>
            Зона доставки
            <select value={zoneId} onChange={(event) => setZoneId(event.target.value)}>
              {zones.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name} · {item.minDays}–{item.maxDays} дн.
                </option>
              ))}
            </select>
          </label>
          {zone?.winterRequiresThermal ? (
            <label className="checkout-thermal">
              <input
                type="checkbox"
                checked={thermal}
                onChange={(event) => setThermal(event.target.checked)}
              />
              Термоупаковка обязательна для этой зоны (+
              {formatPrice(zone.thermalPackMinor, items[0]?.currency ?? 'BYN')})
            </label>
          ) : null}
          <dl className="checkout-total">
            <div>
              <dt>Товары</dt>
              <dd>{formatPrice(subtotal, items[0]?.currency ?? 'BYN')}</dd>
            </div>
            <div>
              <dt>Доставка</dt>
              <dd>{formatPrice(shipping, items[0]?.currency ?? 'BYN')}</dd>
            </div>
            <div>
              <dt>Итого</dt>
              <dd>{formatPrice(subtotal + shipping, items[0]?.currency ?? 'BYN')}</dd>
            </div>
          </dl>
          <button
            type="button"
            disabled={status === 'sending' || !zoneId || authenticated !== true}
            onClick={() => void submit()}
          >
            {status === 'sending' ? 'Оплачиваем…' : 'Оплатить'}
          </button>
          {message ? (
            <p className={status === 'error' ? 'form-message error' : 'form-message'} role="status">
              {message}
            </p>
          ) : null}
        </div>
      </div>
    </section>
  );
}
