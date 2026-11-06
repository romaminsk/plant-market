import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import Link from 'next/link';
import { accessToken } from '@/lib/auth';
import { CartLink } from './CartLink';
import { CookieBanner } from './CookieBanner';
import './globals.css';

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? 'http://localhost:3000'),
  title: 'Plant Market — редкие комнатные растения',
  description: 'Каталог редких комнатных растений с фотографиями конкретного экземпляра.',
  openGraph: {
    title: 'Plant Market — редкие комнатные растения',
    description: 'Каталог редких комнатных растений с фотографиями конкретного экземпляра.',
    type: 'website',
    locale: 'ru_BY',
  },
  robots: { index: true, follow: true },
};

export default async function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  const token = await accessToken();
  return (
    <html lang="ru">
      <body>
        <header className="site-header">
          <Link className="brand" href="/catalog">
            Plant Market
          </Link>
          <nav aria-label="Основная навигация">
            <Link href="/catalog">Каталог</Link>
            <CartLink />
            {token ? <Link href="/account/orders">Мои заказы</Link> : null}
            {token ? (
              <form action="/api/auth/logout" method="post">
                <button type="submit" className="nav-button">
                  Выйти
                </button>
              </form>
            ) : (
              <a href="/api/auth/login?returnTo=/catalog">Войти</a>
            )}
          </nav>
        </header>
        <main className="container">{children}</main>
        <CookieBanner />
      </body>
    </html>
  );
}
