import { fetchPublishedProducts, formatPrice } from '@/lib/api';
import Link from 'next/link';

export const dynamic = 'force-dynamic';

export default async function CatalogPage() {
  const products = await fetchPublishedProducts();

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Phase 0 · каталог</p>
          <h1>Растения в наличии</h1>
          <p className="muted">
            Каждое растение — конкретный экземпляр. Цена указана в белорусских рублях.
          </p>
        </div>
        <span className="count">{products.totalElements} позиций</span>
      </div>

      {products.items.length === 0 ? (
        <p className="empty">Каталог пока пуст. Добавьте продукты через admin API.</p>
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
