'use client';

export default function CatalogError({ reset }: { error: Error; reset: () => void }) {
  return (
    <section className="empty">
      <h1>Каталог недоступен</h1>
      <p className="muted">Не удалось получить данные от catalog-service через gateway.</p>
      <button type="button" onClick={reset}>
        Повторить
      </button>
    </section>
  );
}
