import { hasAnalyticsConsent } from './consent';

export type AnalyticsEvent =
  | 'view_item'
  | 'view_item_list'
  | 'search'
  | 'apply_filter'
  | 'add_to_cart'
  | 'remove_from_cart'
  | 'begin_checkout'
  | 'add_shipping_info'
  | 'add_payment_info'
  | 'purchase'
  | 'restock_subscribe';

interface DataLayerWindow extends Window {
  dataLayer?: Record<string, unknown>[];
}

/** Marketing/analytics events are emitted only after explicit consent (A12). */
export function track(event: AnalyticsEvent, payload: Record<string, unknown> = {}): void {
  if (typeof window === 'undefined' || !hasAnalyticsConsent()) {
    return;
  }
  const target = window as DataLayerWindow;
  target.dataLayer = target.dataLayer ?? [];
  target.dataLayer.push({ event, ...payload, ts: new Date().toISOString() });
}
