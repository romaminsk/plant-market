import Link from 'next/link';
import { fetchProduct, formatPrice } from '@/lib/api';

export const dynamic = 'force-dynamic';

export default async function ProductDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const product = await fetchProduct(id);

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
    </article>
  );
}
