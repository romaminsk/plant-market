'use client';

import { useState } from 'react';
import type { FormEvent } from 'react';
import { track } from '@/lib/analytics';

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

type State = 'idle' | 'sending' | 'done' | 'error';

export function RestockForm({ targetId, code }: { targetId: string; code: string }) {
  const [email, setEmail] = useState('');
  const [state, setState] = useState<State>('idle');
  const [message, setMessage] = useState('');

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setState('sending');
    setMessage('');
    try {
      const response = await fetch(`${API_URL}/api/v1/restock-subscriptions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          targetType: 'INDIVIDUAL_PLANT',
          targetId,
          email: email.trim(),
        }),
      });
      if (!response.ok) {
        const problem = (await response.json().catch(() => null)) as { detail?: string } | null;
        throw new Error(problem?.detail ?? `Не удалось подписаться (HTTP ${response.status})`);
      }
      track('restock_subscribe', { targetId, code });
      setState('done');
      setMessage(`Готово: напишем на ${email.trim()}, когда ${code} снова будет в наличии.`);
    } catch (cause) {
      setState('error');
      setMessage(cause instanceof Error ? cause.message : 'Неизвестная ошибка');
    }
  }

  return (
    <section className="restock-box" aria-labelledby="restock-title">
      <h2 id="restock-title">Появится снова?</h2>
      <p className="muted">
        Оставьте e-mail — отправим одно письмо, когда это растение вернётся в наличие.
      </p>
      <form className="restock-form" onSubmit={onSubmit}>
        <label htmlFor="restock-email">E-mail</label>
        <input
          id="restock-email"
          name="email"
          type="email"
          required
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="you@example.com"
        />
        <button type="submit" disabled={state === 'sending'}>
          {state === 'sending' ? 'Отправляем…' : 'Сообщить о поступлении'}
        </button>
      </form>
      <p className={state === 'error' ? 'form-message error' : 'form-message'} role="status">
        {message}
      </p>
    </section>
  );
}
