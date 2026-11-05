import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import Link from 'next/link';
import './globals.css';

export const metadata: Metadata = {
  title: 'Plant Market — редкие комнатные растения',
  description: 'Каталог редких комнатных растений с фотографиями конкретного экземпляра.',
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
