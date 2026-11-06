'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { formatPrice } from '@/lib/api';
import { cartTotal, readCart, removeFromCart, subscribeToCart } from '@/lib/cart';
import type { CartItem } from '@/lib/cart';

export default function CartPage() {
  const [items, setItems] = useState<CartItem[]>([]);

  useEffect(() => {
    const update = () => setItems(readCart());
    update();
    return subscribeToCart(update);
  }, []);

  return (
    <section>
      <h1>Корзина</h1>
      {items.length === 0 ? (
        <p className="empty">
          Корзина пуста. <Link href="/catalog">Перейти в каталог</Link>.
        </p>
      ) : (
        <>
          <ul className="cart-list">
            {items.map((item) => (
              <li key={`${item.type}-${item.id}`} className="cart-item">
                <div>
                  <h2>{item.name}</h2>
                  <p className="muted">Количество: {item.quantity}</p>
                </div>
                <p className="price">
                  {formatPrice(item.priceMinor * item.quantity, item.currency)}
                </p>
                <button
                  type="button"
                  className="secondary"
                  onClick={() => removeFromCart(item.type, item.id)}
                >
                  Удалить
                </button>
              </li>
            ))}
          </ul>
          <div className="cart-summary">
            <p>
              Итого: <strong>{formatPrice(cartTotal(), items[0]?.currency ?? 'BYN')}</strong>
            </p>
            <Link className="button-link" href="/checkout">
              Перейти к оформлению
            </Link>
          </div>
        </>
      )}
    </section>
  );
}
