'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { addToCart } from '@/lib/cart';
import { track } from '@/lib/analytics';
import type { IndividualPlant } from '@/lib/api';

export function AddToCart({ plant }: { plant: IndividualPlant }) {
  const router = useRouter();
  const [added, setAdded] = useState(false);

  function add(goToCheckout: boolean) {
    addToCart({
      type: 'INDIVIDUAL_PLANT',
      id: plant.id,
      name: `${plant.productName} (${plant.code})`,
      priceMinor: plant.priceMinor,
      currency: plant.currency,
      quantity: 1,
    });
    track('add_to_cart', {
      id: plant.id,
      name: plant.productName,
      priceMinor: plant.priceMinor,
      currency: plant.currency,
    });
    setAdded(true);
    if (goToCheckout) {
      router.push('/checkout');
    }
  }

  return (
    <div className="cart-actions">
      <button type="button" onClick={() => add(false)}>
        {added ? 'В корзине' : 'В корзину'}
      </button>
      <button type="button" className="secondary" onClick={() => add(true)}>
        Купить сейчас
      </button>
    </div>
  );
}
