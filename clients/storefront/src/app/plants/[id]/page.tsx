import Link from 'next/link';
import type { Metadata } from 'next';
import { fetchIndividualPlant, formatPrice } from '@/lib/api';
import { PlantGallery } from './PlantGallery';
import { RestockForm } from './RestockForm';

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
        <PlantGallery
          photos={plant.photos ?? []}
          fallbackUrl={plant.photoUrl}
          name={plant.productName}
          code={plant.code}
        />
        <div>
          <p className="sku">
            {plant.code} · WYSIWYG · {plant.status}
          </p>
          <p className="wysiwyg-badge">Что видите — то и получите</p>
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
            Это конкретный экземпляр: фото общего плана, листа, корней и дефектов закреплены за
            кодом {plant.code}. После продажи позиция исчезает из каталога.
          </p>
          <ul className="trust-list">
            <li>Гарантия 48 часов после получения</li>
            <li>Зимняя доставка: термоупаковка для морозных городов</li>
            <li>Остаток: 1 экземпляр</li>
          </ul>
          {plant.status !== 'AVAILABLE' ? (
            <RestockForm targetId={plant.id} code={plant.code} />
          ) : null}
        </div>
      </div>
    </article>
  );
}
