import Link from 'next/link';
import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { fetchProduct, fetchVariants, formatPrice, productImage } from '@/lib/api';
import { AddVariantToCart } from './AddVariantToCart';
import { safeJsonLd } from '@/lib/seo';
import { TrackView } from '../../TrackView';

export const dynamic = 'force-dynamic';

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  const product = await fetchProduct(id);
  if (product.status !== 'PUBLISHED') {
    notFound();
  }
  return {
    title: `${product.name} — Plant Market`,
    description:
      product.description ?? `${product.name}: фото, уход, свет и цена. Доставка по Беларуси.`,
    alternates: { canonical: `/catalog/${product.id}` },
    openGraph: {
      title: `${product.name} — Plant Market`,
      description: product.description ?? undefined,
      type: 'website',
    },
  };
}

export default async function ProductDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [product, allVariants] = await Promise.all([fetchProduct(id), fetchVariants(id)]);
  if (product.status !== 'PUBLISHED') {
    notFound();
  }
  const variants = allVariants.filter((variant) => variant.status === 'ACTIVE');
  const inStock = variants.some((variant) => variant.availableQuantity > 0);
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: product.name,
    sku: product.sku,
    description: product.description ?? undefined,
    offers: {
      '@type': 'Offer',
      price: (product.priceMinor / 100).toFixed(2),
      priceCurrency: product.currency,
      availability: inStock ? 'https://schema.org/InStock' : 'https://schema.org/OutOfStock',
    },
  };

  return (
    <article className="product-detail">
      <TrackView
        id={product.id}
        name={product.name}
        priceMinor={product.priceMinor}
        currency={product.currency}
      />
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: safeJsonLd(jsonLd) }} />
      <Link className="back-link" href="/catalog">
        ← Назад в каталог
      </Link>
      <div className="detail-layout">
        <div className="detail-photo product-hero">
          <img src={productImage(product.sku)} alt={product.name} width={800} height={600} />
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
