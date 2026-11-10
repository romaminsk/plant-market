import type { Metadata } from 'next';
import { CONSENT_VERSION } from '@/lib/consent';

export const metadata: Metadata = {
  title: 'Cookie и аналитика — Plant Market',
  description: 'Какие cookie использует Plant Market и как управлять согласием.',
  alternates: { canonical: '/legal/cookies' },
};

export default function CookiePolicyPage() {
  return (
    <article className="legal-page">
      <p className="eyebrow">Документ</p>
      <h1>Cookie и аналитика</h1>
      <p className="legal-draft">
        Черновик. Текст описывает фактическую работу сайта, но требует проверки юристом перед
        публикацией как юридического документа.
      </p>

      <h2>Необходимые cookie</h2>
      <p>
        Без них не работают вход, корзина и защита форм. Они не используются для рекламы и
        аналитики.
      </p>
      <ul>
        <li>
          <code>pm.access</code>, <code>pm.refresh</code> — сессия покупателя (httpOnly).
        </li>
        <li>
          <code>pm.csrf</code> — защита оформления заказа от подделки запросов.
        </li>
        <li>
          <code>PM-SESSION</code> — сессия формы входа auth-service.
        </li>
        <li>
          <code>localStorage: pm.cart.v1</code> — корзина; <code>pm.consent.v1</code> — выбор
          согласия; <code>pm.consent.meta.v1</code> — версия политики и дата принятия.
        </li>
      </ul>

      <p>
        Текущая версия политики: <code>{CONSENT_VERSION}</code>. При изменении версии баннер
        появится снова, а аналитика останется выключенной до нового согласия.
      </p>

      <h2>Аналитика</h2>
      <p>
        События (<code>view_item</code>, <code>add_to_cart</code>, <code>begin_checkout</code>,{' '}
        <code>purchase</code>, <code>restock_subscribe</code>) пишутся только после кнопки «Принять
        аналитику». Сторонние трекеры сейчас не подключены; события складываются в
        <code>window.dataLayer</code> и не содержат e-mail, телефона или адреса.
      </p>

      <h2>Как изменить выбор</h2>
      <p>
        Очистите <code>pm.consent.v1</code> и <code>pm.consent.meta.v1</code> в настройках браузера
        и перезагрузите страницу — баннер появится снова. Отказ от аналитики не ограничивает
        покупки.
      </p>
    </article>
  );
}
