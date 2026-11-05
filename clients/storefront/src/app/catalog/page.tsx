import Link from 'next/link';
import type { Metadata } from 'next';
import {
  fetchIndividualPlants,
  fetchPublishedProducts,
  formatPrice,
  type PlantFilters,
} from '@/lib/api';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Каталог растений — Plant Market',
  description: 'Вариегатные, редкие и индивидуальные комнатные растения в наличии.',
  alternates: { canonical: '/catalog' },
};

const LIGHT_OPTIONS = ['LOW', 'MEDIUM', 'BRIGHT', 'DIRECT'] as const;

export default async function CatalogPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const variegated = params.variegated === 'true' ? true : undefined;
  const light = typeof params.light === 'string' && params.light ? params.light : undefined;
  const maxPrice = typeof params.maxPrice === 'string' ? Number(params.maxPrice) : undefined;
  const maxPriceMinor =
    maxPrice !== undefined && Number.isFinite(maxPrice) && maxPrice >= 0
      ? Math.round(maxPrice * 100)
      : undefined;

  const filters: PlantFilters = { variegated, light, maxPriceMinor };
  const [plants, products] = await Promise.all([
    fetchIndividualPlants(filters),
    fetchPublishedProducts(),
  ]);

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Phase 1 · каталог</p>
          <h1>Растения в наличии</h1>
          <p className="muted">
            Каждое индивидуальное растение — конкретный экземпляр с уникальным кодом.
          </p>
        </div>
        <span className="count" aria-live="polite">
          {plants.totalElements} растений
        </span>
      </div>

      <form className="filters" method="get" action="/catalog" aria-label="Фильтры каталога">
        <label className="filter-check">
          <input type="checkbox" name="variegated" value="true" defaultChecked={variegated} />
          Вариегатные
        </label>
        <label>
          Свет
          <select name="light" defaultValue={light ?? ''}>
            <option value="">любой</option>
            {LIGHT_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
        <label>
          Цена до, BYN
          <input
            type="number"
            name="maxPrice"
            min="0"
            step="1"
            defaultValue={maxPrice !== undefined && Number.isFinite(maxPrice) ? maxPrice : ''}
          />
        </label>
        <button type="submit">Показать</button>
        <Link className="reset-link" href="/catalog">
          Сбросить
        </Link>
      </form>

      {plants.items.length === 0 ? (
        <p className="empty">Под выбранные фильтры растений нет.</p>
      ) : (
        <ul className="plant-grid">
          {plants.items.map((plant) => (
            <li key={plant.id} className="plant-card">
              <Link className="plant-photo" href={`/plants/${plant.id}`} aria-label={plant.code}>
                🌿
              </Link>
              <div className="product-body">
                <p className="sku">{plant.code} · WYSIWYG</p>
                <h2>
                  <Link href={`/plants/${plant.id}`}>{plant.productName}</Link>
                </h2>
                {plant.description ? <p className="muted">{plant.description}</p> : null}
                <dl className="facts">
                  <div>
                    <dt>Свет</dt>
                    <dd>{plant.light}</dd>
                  </div>
                  <div>
                    <dt>Уход</dt>
                    <dd>{plant.careLevel}</dd>
                  </div>
                  <div>
                    <dt>Питомцы</dt>
                    <dd>{plant.petSafe ? 'безопасно' : 'осторожно'}</dd>
                  </div>
                </dl>
                <p className="price">{formatPrice(plant.priceMinor, plant.currency)}</p>
              </div>
            </li>
          ))}
        </ul>
      )}

      <div className="section-heading">
        <h2>Позиции каталога</h2>
        <span className="count">{products.totalElements} позиций</span>
      </div>
      {products.items.length === 0 ? (
        <p className="empty">Каталог пока пуст.</p>
      ) : (
        <ul className="product-grid">
          {products.items.map((product) => (
            <li key={product.id} className="product-card">
              <div className="product-photo" aria-hidden="true">
                🌿
              </div>
              <div className="product-body">
                <p className="sku">{product.sku}</p>
                <h2>
                  <Link href={`/catalog/${product.id}`}>{product.name}</Link>
                </h2>
                {product.description ? <p className="muted">{product.description}</p> : null}
                <dl className="facts">
                  <div>
                    <dt>Свет</dt>
                    <dd>{product.light}</dd>
                  </div>
                  <div>
                    <dt>Уход</dt>
                    <dd>{product.careLevel}</dd>
                  </div>
                  <div>
                    <dt>Питомцы</dt>
                    <dd>{product.petSafe ? 'безопасно' : 'осторожно'}</dd>
                  </div>
                </dl>
                <p className="price">{formatPrice(product.priceMinor, product.currency)}</p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
