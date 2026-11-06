import Link from 'next/link';
import type { Metadata } from 'next';
import { fetchIndividualPlant, formatPrice } from '@/lib/api';
import { accessToken } from '@/lib/auth';
import { CheckoutForm } from './CheckoutForm';

export const dynamic = 'force-dynamic';

export const metadata: Metadata = {
  title: 'Оформление заказа — Plant Market',
  robots: { index: false, follow: false },
};

export default async function CheckoutPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const plantId = typeof params.plantId === 'string' ? params.plantId : '';
  if (!plantId) {
    return (
      <section>
        <h1>Оформление заказа</h1>
        <p className="empty">
          В корзине пусто. <Link href="/catalog">Вернуться в каталог</Link>.
        </p>
      </section>
    );
  }
  const plant = await fetchIndividualPlant(plantId);
  const token = await accessToken();

  return (
    <section>
      <Link className="back-link" href={`/plants/${plant.id}`}>
        ← Назад к растению
      </Link>
      <h1>Оформление заказа</h1>
      <div className="checkout-layout">
        <div className="checkout-summary">
          <p className="sku">
            {plant.code} · WYSIWYG · {plant.status}
          </p>
          <h2>{plant.productName}</h2>
          <p className="price">{formatPrice(plant.priceMinor, plant.currency)}</p>
          {plant.status !== 'AVAILABLE' ? (
            <p className="form-message error">Это растение уже недоступно.</p>
          ) : null}
        </div>
        {plant.status !== 'AVAILABLE' ? null : token ? (
          <CheckoutForm
            plantId={plant.id}
            priceMinor={plant.priceMinor}
            currency={plant.currency}
          />
        ) : (
          <div className="checkout-result" role="status">
            <h2>Войдите, чтобы оформить заказ</h2>
            <p className="muted">Оплата и доставка доступны после входа.</p>
            <a
              className="button-link"
              href={`/api/auth/login?returnTo=/checkout?plantId=${plant.id}`}
            >
              Войти
            </a>
          </div>
        )}
      </div>
    </section>
  );
}
