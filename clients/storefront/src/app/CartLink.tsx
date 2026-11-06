'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { cartCount, subscribeToCart } from '@/lib/cart';

export function CartLink() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const update = () => setCount(cartCount());
    update();
    return subscribeToCart(update);
  }, []);

  return <Link href="/cart">Корзина{count > 0 ? ` (${count})` : ''}</Link>;
}
