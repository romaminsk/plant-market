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

export type PlantPhotoKind = 'GENERAL' | 'LEAF' | 'ROOTS' | 'DEFECTS';

export interface PlantPhoto {
  url: string;
  kind: PlantPhotoKind;
}

export interface IndividualPlant {
  id: string;
  productId: string;
  productName: string;
  variegated: boolean;
  code: string;
  description: string | null;
  photoUrl: string | null;
  photos: PlantPhoto[];
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

export interface Variant {
  id: string;
  productId: string;
  sku: string;
  name: string;
  size: string | null;
  potSize: string | null;
  form: string | null;
  priceMinor: number;
  currency: string;
  status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
  availableQuantity: number;
}

export interface ShippingZone {
  id: string;
  name: string;
  city: string;
  priceMinor: number;
  freeFromMinor: number;
  minDays: number;
  maxDays: number;
  winterRequiresThermal: boolean;
  thermalPackMinor: number;
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

export async function fetchVariants(productId: string): Promise<Variant[]> {
  const response = await fetch(`${API_URL}/api/v1/products/${productId}/variants`, {
    cache: 'no-store',
  });
  if (!response.ok) {
    throw new Error(`Variants request failed with status ${response.status}`);
  }
  return (await response.json()) as Variant[];
}

export async function fetchShippingZones(): Promise<ShippingZone[]> {
  const response = await fetch(`${API_URL}/api/v1/shipping/zones`, { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Shipping zones request failed with status ${response.status}`);
  }
  return (await response.json()) as ShippingZone[];
}

export function formatPrice(priceMinor: number, currency: string): string {
  return new Intl.NumberFormat('be-BY', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(priceMinor / 100);
}
