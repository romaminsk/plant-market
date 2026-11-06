'use client';

import { useEffect } from 'react';
import { track } from '@/lib/analytics';

export function TrackView({
  id,
  name,
  priceMinor,
  currency,
}: {
  id: string;
  name: string;
  priceMinor: number;
  currency: string;
}) {
  useEffect(() => {
    track('view_item', { id, name, priceMinor, currency });
  }, [id, name, priceMinor, currency]);
  return null;
}
