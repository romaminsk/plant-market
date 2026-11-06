import Link from 'next/link';
import { fetchProduct, fetchVariants, formatPrice } from '@/lib/api';
import { AddVariantToCart } from './AddVariantToCart';

export const dynamic = 'force-dynamic';

export default async function ProductDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [product, variants] = await Promise.all([fetchProduct(id), fetchVariants(id)]);

  return (
    <article className="product-detail">
      <Link className="back-link" href="/catalog">
        ← Назад в каталог
      </Link>
      <div className="detail-layout">
        <div className="detail-photo" aria-hidden="true">
          🌿
        </div>
        <div>
          <p className="sku">{product.sku}</p>
          <h1>{product.name}</h1>
          {product.description ? <p className="muted">{product.description}</p> : null}
          <p className="price detail-price" data-testid="product-price">
            {formatPrice(product.priceMinor, product.currency)}
          </p>
          <dl className="facts detail-facts">
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
        </div>
      </div>
      {variants.length > 0 ? (
        <section className="variant-section">
          <h2>Варианты</h2>
          <ul className="variant-list">
            {variants.map((variant) => (
              <li key={variant.id} className="variant-card">
                <div>
                  <h3>{variant.name}</h3>
                  <p className="muted">
                    {[variant.size, variant.potSize, variant.form].filter(Boolean).join(' · ')}
                  </p>
                  <p className="muted">
                    {variant.availableQuantity > 0
                      ? `В наличии: ${variant.availableQuantity}`
                      : 'Нет в наличии'}
                  </p>
                </div>
                <p className="price">{formatPrice(variant.priceMinor, variant.currency)}</p>
                <AddVariantToCart variant={variant} />
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </article>
  );
}
