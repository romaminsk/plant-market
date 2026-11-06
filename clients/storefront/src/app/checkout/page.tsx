'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { fetchShippingZones, formatPrice } from '@/lib/api';
import type { ShippingZone } from '@/lib/api';
import { clearCart, readCart } from '@/lib/cart';
import { track } from '@/lib/analytics';
import type { CartItem } from '@/lib/cart';

export default function CheckoutPage() {
  const [items, setItems] = useState<CartItem[]>([]);
  const [zones, setZones] = useState<ShippingZone[]>([]);
  const [zoneId, setZoneId] = useState('');
  const [thermal, setThermal] = useState(false);
  const [address, setAddress] = useState('');
  const [email, setEmail] = useState('');
  const [authenticated, setAuthenticated] = useState<boolean | null>(null);
  const [status, setStatus] = useState<'idle' | 'sending' | 'done' | 'error'>('idle');
  const [message, setMessage] = useState('');
  const [orderId, setOrderId] = useState('');
  const [paidTotal, setPaidTotal] = useState<number | null>(null);

  useEffect(() => {
    const loaded = readCart();
    setItems(loaded);
    if (loaded.length > 0) {
      track('begin_checkout', { items: loaded.length });
    }
    fetchShippingZones()
      .then((loaded) => {
        setZones(loaded);
        if (loaded.length > 0) {
          const preferred = loaded.find((zone) => !zone.winterRequiresThermal) ?? loaded[0];
          setZoneId(preferred.id);
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
  const thermalRequired = Boolean(zone?.winterRequiresThermal);
  const thermalMissing = thermalRequired && !thermal;
  const guest = authenticated === false;
  const guestEmailValid = /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.trim());

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
          deliveryAddress: address,
          guestEmail: guest ? email.trim() : undefined,
        }),
      });
      const payload = (await response.json().catch(() => null)) as {
        detail?: string;
        orderId?: string;
        status?: string;
        totalMinor?: number;
      } | null;
      if (response.status === 401) {
        setAuthenticated(false);
        setStatus('idle');
        return;
      }
      if (!response.ok) {
        throw new Error(payload?.detail ?? `Оплата не прошла (HTTP ${response.status})`);
      }
      track('purchase', {
        orderId: payload?.orderId,
        value: items.reduce((total, item) => total + item.priceMinor * item.quantity, 0),
      });
      clearCart();
      setOrderId(payload?.orderId ?? '');
      setPaidTotal(payload?.totalMinor ?? null);
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
        {paidTotal !== null ? (
          <p>
            Оплачено: <strong>{formatPrice(paidTotal, items[0]?.currency ?? 'BYN')}</strong>
          </p>
        ) : null}
        <p className="muted">
          {guest && email
            ? `Подтверждение отправлено на ${email}. Спасибо!`
            : 'Подтверждение отправлено на e-mail. Спасибо!'}
        </p>
        {guest ? (
          <p className="muted">Сохраните номер заказа — он понадобится для связи.</p>
        ) : (
          <p>
            <Link href="/account/orders">Мои заказы</Link>
          </p>
        )}
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
          {guest ? (
            <div className="guest-checkout">
              <p className="eyebrow">Быстрый заказ</p>
              <label>
                E-mail для подтверждения
                <input
                  type="email"
                  name="guestEmail"
                  autoComplete="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="you@example.com"
                  required
                />
              </label>
              <p className="muted">
                Оформим заказ без аккаунта — подтверждение придёт на этот e-mail.
              </p>
              <a className="checkout-login" href="/api/auth/login?returnTo=/checkout">
                Войти в аккаунт
              </a>
            </div>
          ) : null}
          <label>
            Адрес доставки
            <textarea
              value={address}
              onChange={(event) => setAddress(event.target.value)}
              rows={2}
              placeholder="Город, улица, дом, квартира"
              required
            />
          </label>
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
          {thermalMissing ? (
            <p className="form-message error" role="status">
              Для этой зоны обязательна термоупаковка.
            </p>
          ) : null}
          <button
            type="button"
            disabled={
              status === 'sending' ||
              !zoneId ||
              authenticated === null ||
              (guest && !guestEmailValid) ||
              address.trim().length === 0 ||
              thermalMissing
            }
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
