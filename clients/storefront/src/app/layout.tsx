import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import Link from 'next/link';
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

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="ru">
      <body>
        <header className="site-header">
          <Link className="brand" href="/catalog">
            Plant Market
          </Link>
          <nav aria-label="Основная навигация">
            <Link href="/catalog">Каталог</Link>
          </nav>
        </header>
        <main className="container">{children}</main>
      </body>
    </html>
  );
}
