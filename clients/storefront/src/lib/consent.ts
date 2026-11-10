export type Consent = 'all' | 'essential';

export const CONSENT_VERSION = '2026-11-10';

const KEY = 'pm.consent.v1';
const META_KEY = 'pm.consent.meta.v1';
const EVENT = 'pm-consent-changed';

interface ConsentMeta {
  version: string;
  acceptedAt: string;
}

function readMeta(): ConsentMeta | null {
  try {
    const parsed = JSON.parse(window.localStorage.getItem(META_KEY) ?? 'null') as unknown;
    if (typeof parsed !== 'object' || parsed === null) {
      return null;
    }
    const candidate = parsed as Partial<ConsentMeta>;
    return typeof candidate.version === 'string' && typeof candidate.acceptedAt === 'string'
      ? { version: candidate.version, acceptedAt: candidate.acceptedAt }
      : null;
  } catch {
    return null;
  }
}

export function readConsent(): Consent | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const value = window.localStorage.getItem(KEY);
  if (value !== 'all' && value !== 'essential') {
    return null;
  }
  const meta = readMeta();
  return meta?.version === CONSENT_VERSION ? value : null;
}

export function writeConsent(consent: Consent): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(KEY, consent);
  window.localStorage.setItem(
    META_KEY,
    JSON.stringify({ version: CONSENT_VERSION, acceptedAt: new Date().toISOString() }),
  );
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
