export interface Product {
  id: string;
  sku: string;
  name: string;
  slug: string;
  description: string | null;
  categoryId: string;
  priceMinor: number;
  currency: 'BYN';
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  careLevel: 'EASY' | 'MEDIUM' | 'HARD';
  light: 'LOW' | 'MEDIUM' | 'BRIGHT' | 'DIRECT';
  petSafe: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductPage {
  items: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export async function fetchPublishedProducts(): Promise<ProductPage> {
  const response = await fetch(`${API_URL}/api/v1/products?page=0&size=50&status=PUBLISHED`, {
    cache: 'no-store',
  });
  if (!response.ok) {
    throw new Error(`Catalog request failed with status ${response.status}`);
  }
  return (await response.json()) as ProductPage;
}

export function formatPrice(priceMinor: number, currency: string): string {
  return new Intl.NumberFormat('be-BY', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(priceMinor / 100);
}
