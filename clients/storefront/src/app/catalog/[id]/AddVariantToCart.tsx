'use client';

import { useState } from 'react';
import { addToCart } from '@/lib/cart';
import { track } from '@/lib/analytics';
import type { Variant } from '@/lib/api';

export function AddVariantToCart({ variant }: { variant: Variant }) {
  const [added, setAdded] = useState(false);

  return (
    <button
      type="button"
      disabled={variant.availableQuantity <= 0}
      onClick={() => {
        addToCart({
          type: 'VARIANT',
          id: variant.id,
          name: `${variant.name} (${variant.sku})`,
          priceMinor: variant.priceMinor,
          currency: variant.currency,
          quantity: 1,
        });
        track('add_to_cart', {
          id: variant.id,
          name: variant.name,
          priceMinor: variant.priceMinor,
          currency: variant.currency,
        });
        setAdded(true);
      }}
    >
      {variant.availableQuantity <= 0 ? 'Нет в наличии' : added ? 'В корзине' : 'В корзину'}
    </button>
  );
}
