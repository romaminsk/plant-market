export type Consent = 'all' | 'essential';

const KEY = 'pm.consent.v1';
const EVENT = 'pm-consent-changed';

export function readConsent(): Consent | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const value = window.localStorage.getItem(KEY);
  return value === 'all' || value === 'essential' ? value : null;
}

export function writeConsent(consent: Consent): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(KEY, consent);
  window.dispatchEvent(new Event(EVENT));
}

export function hasAnalyticsConsent(): boolean {
  return readConsent() === 'all';
}

export function subscribeToConsent(listener: () => void): () => void {
  if (typeof window === 'undefined') {
    return () => {};
  }
  window.addEventListener(EVENT, listener);
  window.addEventListener('storage', listener);
  return () => {
    window.removeEventListener(EVENT, listener);
    window.removeEventListener('storage', listener);
  };
}
