'use client';

import { useEffect, useState } from 'react';
import { fetchShippingZones, formatPrice } from '@/lib/api';
import type { ShippingZone } from '@/lib/api';

export function CheckoutForm({
  plantId,
  priceMinor,
  currency,
}: {
  plantId: string;
  priceMinor: number;
  currency: string;
}) {
  const [zones, setZones] = useState<ShippingZone[]>([]);
  const [zoneId, setZoneId] = useState('');
  const [thermal, setThermal] = useState(false);
  const [status, setStatus] = useState<'idle' | 'sending' | 'done' | 'error' | 'auth'>('idle');
  const [message, setMessage] = useState('');
  const [orderId, setOrderId] = useState('');

  useEffect(() => {
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
  }, []);

  const zone = zones.find((item) => item.id === zoneId);
  const shipping = zone
    ? (priceMinor >= zone.freeFromMinor ? 0 : zone.priceMinor) +
      (thermal ? zone.thermalPackMinor : 0)
    : 0;

  async function submit() {
    setStatus('sending');
    setMessage('');
    try {
      const response = await fetch('/api/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ plantId, shippingZoneId: zoneId, thermalPack: thermal }),
      });
      const payload = (await response.json().catch(() => null)) as {
        detail?: string;
        orderId?: string;
        status?: string;
      } | null;
      if (response.status === 401) {
        setStatus('auth');
        return;
      }
      if (!response.ok) {
        throw new Error(payload?.detail ?? `Оплата не прошла (HTTP ${response.status})`);
      }
      setStatus('done');
      setOrderId(payload?.orderId ?? '');
    } catch (cause) {
      setStatus('error');
      setMessage(cause instanceof Error ? cause.message : 'Неизвестная ошибка');
    }
  }

  if (status === 'done') {
    return (
      <div className="checkout-result" role="status">
        <h2>Заказ оплачен</h2>
        <p>
          Номер заказа: <span className="mono">{orderId}</span>
        </p>
        <p className="muted">Подтверждение отправлено на e-mail. Спасибо!</p>
      </div>
    );
  }

  if (status === 'auth') {
    return (
      <div className="checkout-result" role="status">
        <h2>Нужно войти</h2>
        <p className="muted">Войдите, чтобы оформить и оплатить заказ.</p>
        <a className="button-link" href={`/api/auth/login?returnTo=/checkout?plantId=${plantId}`}>
          Войти
        </a>
      </div>
    );
  }

  return (
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
          {formatPrice(zone.thermalPackMinor, currency)})
        </label>
      ) : null}
      <dl className="checkout-total">
        <div>
          <dt>Растение</dt>
          <dd>{formatPrice(priceMinor, currency)}</dd>
        </div>
        <div>
          <dt>Доставка</dt>
          <dd>{formatPrice(shipping, currency)}</dd>
        </div>
        <div>
          <dt>Итого</dt>
          <dd>{formatPrice(priceMinor + shipping, currency)}</dd>
        </div>
      </dl>
      <button
        type="button"
        disabled={status === 'sending' || !zoneId}
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
  );
}
