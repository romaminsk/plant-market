import { useEffect, useState } from 'react';
import { decideClaim, fetchClaims } from './api';
import type { Claim, ClaimDecision } from './api';

export function ClaimsPanel({ accessToken }: { accessToken: string }) {
  const [claims, setClaims] = useState<Claim[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchClaims(accessToken)
      .then((loaded) => {
        if (!cancelled) {
          setClaims(loaded);
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setError(cause instanceof Error ? cause.message : 'Неизвестная ошибка');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  function replace(updated: Claim) {
    setClaims((previous) => previous.map((claim) => (claim.id === updated.id ? updated : claim)));
  }

  return (
    <section className="card panel">
      <h2>Претензии</h2>
      <p className="muted">
        Решение оператора: замена, возврат или отказ. Клиент получает письмо после решения.
      </p>
      {error ? <p className="error">{error}</p> : null}
      {loading ? <p className="muted">Загрузка…</p> : null}
      {!loading && claims.length === 0 ? <p className="empty">Открытых претензий нет.</p> : null}
      {claims.length > 0 ? (
        <table>
          <thead>
            <tr>
              <th>Претензия</th>
              <th>Заказ</th>
              <th>Причина</th>
              <th>Фото</th>
              <th>Статус</th>
              <th>Решение</th>
            </tr>
          </thead>
          <tbody>
            {claims.map((claim) => (
              <ClaimRow
                key={claim.id}
                claim={claim}
                accessToken={accessToken}
                onDecided={replace}
              />
            ))}
          </tbody>
        </table>
      ) : null}
    </section>
  );
}

function ClaimRow({
  claim,
  accessToken,
  onDecided,
}: {
  claim: Claim;
  accessToken: string;
  onDecided: (claim: Claim) => void;
}) {
  const [decision, setDecision] = useState<ClaimDecision>('REPLACEMENT');
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    setBusy(true);
    setError(null);
    try {
      onDecided(await decideClaim(accessToken, claim.id, decision, comment || null));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Не удалось сохранить решение');
    } finally {
      setBusy(false);
    }
  }

  return (
    <tr>
      <td className="mono" title={claim.id}>
        {claim.id.slice(-8)}
      </td>
      <td className="mono" title={claim.orderId}>
        {claim.orderId.slice(-8)}
      </td>
      <td>{claim.reason}</td>
      <td>
        {claim.photoUrls.map((url, index) => (
          <a key={url} href={url} target="_blank" rel="noreferrer">
            фото {index + 1}
          </a>
        ))}
      </td>
      <td>{claim.status}</td>
      <td>
        {claim.status === 'OPEN' ? (
          <div className="claim-decision">
            <select
              aria-label={`Решение по претензии ${claim.id.slice(-8)}`}
              value={decision}
              onChange={(event) => setDecision(event.target.value as ClaimDecision)}
            >
              <option value="REPLACEMENT">Замена</option>
              <option value="REFUND">Возврат</option>
              <option value="REJECT">Отказ</option>
            </select>
            <input
              aria-label={`Комментарий по претензии ${claim.id.slice(-8)}`}
              placeholder="Комментарий"
              value={comment}
              onChange={(event) => setComment(event.target.value)}
            />
            <button type="button" disabled={busy} onClick={() => void submit()}>
              {busy ? 'Сохраняем…' : 'Решить'}
            </button>
            {error ? <span className="error">{error}</span> : null}
          </div>
        ) : (
          <span className="muted">{claim.decisionComment ?? '—'}</span>
        )}
      </td>
    </tr>
  );
}
