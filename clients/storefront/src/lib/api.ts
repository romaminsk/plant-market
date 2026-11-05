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

export interface IndividualPlant {
  id: string;
  productId: string;
  productName: string;
  variegated: boolean;
  code: string;
  description: string | null;
  photoUrl: string | null;
  priceMinor: number;
  currency: 'BYN';
  status:
    | 'ACQUIRED'
    | 'QUARANTINE'
    | 'AVAILABLE'
    | 'RESERVED'
    | 'SOLD'
    | 'DELIVERED'
    | 'RETURNED'
    | 'MORTALITY';
  careLevel: 'EASY' | 'MEDIUM' | 'HARD';
  light: 'LOW' | 'MEDIUM' | 'BRIGHT' | 'DIRECT';
  petSafe: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface IndividualPlantPage {
  items: IndividualPlant[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PlantFilters {
  variegated?: boolean;
  light?: string;
  maxPriceMinor?: number;
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

export async function fetchProduct(id: string): Promise<Product> {
  const response = await fetch(`${API_URL}/api/v1/products/${id}`, { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Product request failed with status ${response.status}`);
  }
  return (await response.json()) as Product;
}

export async function fetchIndividualPlants(
  filters: PlantFilters = {},
): Promise<IndividualPlantPage> {
  const params = new URLSearchParams({ status: 'AVAILABLE', size: '50' });
  if (filters.variegated !== undefined) {
    params.set('variegated', String(filters.variegated));
  }
  if (filters.light) {
    params.set('light', filters.light);
  }
  if (filters.maxPriceMinor !== undefined) {
    params.set('maxPriceMinor', String(filters.maxPriceMinor));
  }
  const response = await fetch(`${API_URL}/api/v1/individual-plants?${params}`, {
    cache: 'no-store',
  });
  if (!response.ok) {
    throw new Error(`Individual plants request failed with status ${response.status}`);
  }
  return (await response.json()) as IndividualPlantPage;
}

export async function fetchIndividualPlant(id: string): Promise<IndividualPlant> {
  const response = await fetch(`${API_URL}/api/v1/individual-plants/${id}`, {
    cache: 'no-store',
  });
  if (!response.ok) {
    throw new Error(`Individual plant request failed with status ${response.status}`);
  }
  return (await response.json()) as IndividualPlant;
}

export function formatPrice(priceMinor: number, currency: string): string {
  return new Intl.NumberFormat('be-BY', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(priceMinor / 100);
}
