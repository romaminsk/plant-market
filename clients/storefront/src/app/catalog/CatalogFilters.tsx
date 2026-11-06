'use client';

import { useState, useTransition } from 'react';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';

const LIGHT_OPTIONS = [
  { value: '', label: 'Любой' },
  { value: 'LOW', label: 'Низкий' },
  { value: 'MEDIUM', label: 'Средний' },
  { value: 'BRIGHT', label: 'Яркий' },
  { value: 'DIRECT', label: 'Прямой' },
] as const;

export function CatalogFilters({
  variegated,
  light,
  maxPrice,
}: {
  variegated?: boolean;
  light?: string;
  maxPrice?: number;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [price, setPrice] = useState(maxPrice?.toString() ?? '');
  const [isPending, startTransition] = useTransition();

  function apply(updates: Record<string, string | undefined>) {
    const params = new URLSearchParams(searchParams.toString());
    for (const [key, value] of Object.entries(updates)) {
      if (value) {
        params.set(key, value);
      } else {
        params.delete(key);
      }
    }
    const query = params.toString();
    startTransition(() => {
      router.push(query ? `${pathname}?${query}` : pathname, { scroll: false });
    });
  }

  function applyPrice() {
    const value = Number(price);
    if (price.trim() === '' || !Number.isFinite(value) || value < 0) {
      apply({ maxPrice: undefined });
      return;
    }
    apply({ maxPrice: String(Math.round(value)) });
  }

  const activeCount =
    Number(Boolean(variegated)) + Number(Boolean(light)) + Number(maxPrice !== undefined);

  return (
    <aside className="filter-panel" aria-label="Фильтры каталога">
      <div className="filter-head">
        <div>
          <p className="eyebrow">Подбор</p>
          <h2>Фильтры</h2>
        </div>
        <span className="filter-count">{activeCount > 0 ? `${activeCount} выбрано` : 'все'}</span>
      </div>

      <div className="filter-group">
        <p className="filter-label">Вариегатность</p>
        <button
          type="button"
          className={variegated ? 'filter-toggle active' : 'filter-toggle'}
          aria-pressed={Boolean(variegated)}
          onClick={() => apply({ variegated: variegated ? undefined : 'true' })}
        >
          <span>Только вариегатные</span>
          <span className="switch" aria-hidden="true" />
        </button>
      </div>

      <div className="filter-group">
        <p className="filter-label">Свет</p>
        <div className="filter-pills" role="group" aria-label="Уровень света">
          {LIGHT_OPTIONS.map((option) => (
            <button
              key={option.value || 'any'}
              type="button"
              className={
                light === option.value || (!light && !option.value)
                  ? 'filter-pill active'
                  : 'filter-pill'
              }
              aria-pressed={light === option.value || (!light && !option.value)}
              onClick={() => apply({ light: option.value || undefined })}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      <div className="filter-group">
        <label className="filter-label" htmlFor="filter-price">
          Цена до, BYN
        </label>
        <div className="filter-price">
          <input
            id="filter-price"
            type="number"
            min="0"
            step="1"
            inputMode="numeric"
            value={price}
            onChange={(event) => setPrice(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault();
                applyPrice();
              }
            }}
            placeholder="Например, 150"
          />
          <button type="button" onClick={applyPrice}>
            ОК
          </button>
        </div>
      </div>

      <button
        type="button"
        className="filter-reset"
        onClick={() => {
          setPrice('');
          startTransition(() => router.push(pathname, { scroll: false }));
        }}
      >
        Сбросить фильтры
      </button>
      {isPending ? <p className="filter-pending">Обновляем…</p> : null}
    </aside>
  );
}
