'use client';

import { useEffect, useState } from 'react';
import { CONSENT_VERSION, readConsent, subscribeToConsent, writeConsent } from '@/lib/consent';
import type { Consent } from '@/lib/consent';

export function CookieBanner() {
  const [consent, setConsent] = useState<Consent | null>('all');

  useEffect(() => {
    const update = () => setConsent(readConsent());
    update();
    return subscribeToConsent(update);
  }, []);

  if (consent !== null) {
    return null;
  }

  function choose(value: Consent) {
    writeConsent(value);
    setConsent(value);
  }

  return (
    <aside
      className="cookie-banner"
      role="dialog"
      aria-label="Cookie и аналитика"
      data-consent-version={CONSENT_VERSION}
    >
      <p>
        Используем необходимые cookie для входа и корзины. Аналитику включаем только с вашего
        согласия.
      </p>
      <div className="cookie-actions">
        <button type="button" onClick={() => choose('all')}>
          Принять аналитику
        </button>
        <button type="button" className="secondary" onClick={() => choose('essential')}>
          Только необходимые
        </button>
      </div>
    </aside>
  );
}
