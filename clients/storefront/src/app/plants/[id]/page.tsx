import Link from 'next/link';
import type { Metadata } from 'next';
import { fetchIndividualPlant, formatPrice } from '@/lib/api';

export const dynamic = 'force-dynamic';

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  const plant = await fetchIndividualPlant(id);
  return {
    title: `${plant.productName} (${plant.code}) — Plant Market`,
    description:
      plant.description ??
      `Индивидуальное растение ${plant.productName}, код ${plant.code}, WYSIWYG.`,
    alternates: { canonical: `/plants/${plant.id}` },
  };
}

export default async function IndividualPlantPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const plant = await fetchIndividualPlant(id);

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
          <p className="sku">
            {plant.code} · WYSIWYG · {plant.status}
          </p>
          <h1>{plant.productName}</h1>
          {plant.description ? <p className="muted">{plant.description}</p> : null}
          <p className="price detail-price" data-testid="plant-price">
            {formatPrice(plant.priceMinor, plant.currency)}
          </p>
          <dl className="facts detail-facts">
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
          <p className="muted">
            Это конкретный экземпляр. Фото и код закреплены за растением; после продажи позиция
            исчезает из каталога.
          </p>
        </div>
      </div>
    </article>
  );
}
