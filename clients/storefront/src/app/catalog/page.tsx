import Link from 'next/link';
import type { Metadata } from 'next';
import {
  fetchAllPublishedProducts,
  fetchIndividualPlants,
  formatPrice,
  productImage,
  type PlantFilters,
} from '@/lib/api';
import { CatalogFilters } from './CatalogFilters';
import { pluralRu } from '@/lib/plural';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Каталог растений — Plant Market',
  description: 'Вариегатные, редкие и индивидуальные комнатные растения в наличии.',
  alternates: { canonical: '/catalog' },
};

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

  const pageParam = typeof params.page === 'string' ? Number(params.page) : 0;
  const page = Number.isInteger(pageParam) && pageParam > 0 ? pageParam : 0;
  const filters: PlantFilters = { variegated, light, maxPriceMinor };
  const filtersActive = Boolean(variegated || light || maxPriceMinor !== undefined);
  const [plants, products, allPlants] = await Promise.all([
    fetchIndividualPlants(filters, page),
    fetchAllPublishedProducts(),
    filtersActive ? fetchIndividualPlants({}) : Promise.resolve(null),
  ]);
  const totalAvailable = allPlants?.totalElements ?? plants.totalElements;
  const pageHref = (target: number) => {
    const query = new URLSearchParams();
    if (variegated) {
      query.set('variegated', 'true');
    }
    if (light) {
      query.set('light', light);
    }
    if (maxPrice !== undefined) {
      query.set('maxPrice', String(maxPrice));
    }
    if (target > 0) {
      query.set('page', String(target));
    }
    const value = query.toString();
    return value ? `/catalog?${value}` : '/catalog';
  };

  return (
    <section>
      <div className="catalog-hero">
        <img className="catalog-hero-image" src="/images/plants/anthurium.jpg" alt="" />
        <div className="catalog-hero-copy">
          <p className="eyebrow">Редкие растения · Минск</p>
          <h1>Растения, которые не повторить.</h1>
          <p>
            Каждый экземпляр снят и промаркирован. Вы получаете именно его — с корнями, листьями и
            историей.
          </p>
          <a className="hero-cta" href="#catalog">
            Открыть каталог →
          </a>
          <span className="hero-meta" aria-live="polite">
            <strong>{totalAvailable}</strong>
            {pluralRu(totalAvailable, 'растение', 'растения', 'растений')} в наличии
          </span>
        </div>
      </div>

      <div id="catalog" className="catalog-results">
        <CatalogFilters variegated={variegated} light={light} maxPrice={maxPrice} />

        <div className="results-bar">
          <p>
            Найдено <strong>{plants.totalElements}</strong>
          </p>
          <span className="muted">Фото и код закреплены за экземпляром</span>
        </div>

        {plants.items.length === 0 ? (
          <div className="empty">
            <h2>Под эти фильтры растений нет</h2>
            <p>Попробуйте убрать свет или увеличить бюджет.</p>
            <Link className="button-link" href="/catalog">
              Сбросить фильтры
            </Link>
          </div>
        ) : (
          <ul className="plant-grid">
            {plants.items.map((plant) => (
              <li key={plant.id} className="plant-card">
                <Link
                  className="plant-photo"
                  href={`/plants/${plant.code}`}
                  aria-label={plant.code}
                >
                  <img
                    src={plant.photos?.[0]?.url ?? plant.photoUrl ?? '/images/plants/monstera.jpg'}
                    alt={`${plant.productName}, ${plant.code}`}
                    width={400}
                    height={300}
                    loading="eager"
                  />
                  <span className="photo-badge">WYSIWYG</span>
                </Link>
                <div className="product-body">
                  <p className="sku">{plant.code}</p>
                  <h2>
                    <Link href={`/plants/${plant.code}`}>{plant.productName}</Link>
                  </h2>
                  <p className="muted plant-description">{plant.description}</p>
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

        {plants.totalPages > 1 ? (
          <nav className="pagination" aria-label="Страницы каталога">
            {page > 0 ? (
              <Link className="button-link" href={pageHref(page - 1)}>
                ← Назад
              </Link>
            ) : (
              <span />
            )}
            <span className="muted">
              Страница {page + 1} из {plants.totalPages}
            </span>
            {page + 1 < plants.totalPages ? (
              <Link className="button-link" href={pageHref(page + 1)}>
                Следующая →
              </Link>
            ) : (
              <span />
            )}
          </nav>
        ) : null}

        {!filtersActive ? (
          <>
            <div className="section-heading">
              <div>
                <p className="eyebrow">Не только экземпляры</p>
                <h2>Позиции каталога</h2>
              </div>
              <span className="count">
                {products.length} {pluralRu(products.length, 'позиция', 'позиции', 'позиций')}
              </span>
            </div>
            {products.length === 0 ? (
              <p className="empty">Каталог пока пуст.</p>
            ) : (
              <ul className="product-list">
                {products.map((product) => (
                  <li key={product.id} className="product-row">
                    <div className="product-row-photo">
                      <img
                        src={product.imageUrl ?? productImage(product.sku)}
                        alt={product.name}
                        width={200}
                        height={200}
                        loading="lazy"
                      />
                    </div>
                    <div className="product-row-body">
                      <p className="sku">{product.sku}</p>
                      <h2>
                        <Link href={`/catalog/${product.slug}`}>{product.name}</Link>
                      </h2>
                      <p className="muted plant-description">{product.description}</p>
                      <div className="product-row-foot">
                        <span className="muted">
                          {product.light} · {product.careLevel} ·{' '}
                          {product.petSafe ? 'безопасно' : 'осторожно'}
                        </span>
                        <p className="price">{formatPrice(product.priceMinor, product.currency)}</p>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </>
        ) : null}
      </div>
    </section>
  );
}
