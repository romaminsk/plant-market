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

async function fetchJson<T>(url: string, label: string): Promise<T> {
  for (let attempt = 0; attempt < 2; attempt++) {
    const response = await fetch(url, { cache: 'no-store' });
    if (response.ok) {
      return (await response.json()) as T;
    }
    if (attempt === 0 && (response.status === 429 || response.status >= 500)) {
      await new Promise((resolve) => setTimeout(resolve, 250));
      continue;
    }
    throw new Error(`${label} request failed with status ${response.status}`);
  }
  throw new Error(`${label} request failed`);
}

export async function fetchPublishedProducts(page = 0, size = 50): Promise<ProductPage> {
  return fetchJson<ProductPage>(
    `${API_URL}/api/v1/products?page=${page}&size=${size}&status=PUBLISHED`,
    'Catalog',
  );
}

export async function fetchAllPublishedProducts(): Promise<Product[]> {
  return collectPages((page) => fetchPublishedProducts(page));
}

export async function fetchProduct(id: string): Promise<Product> {
  return fetchJson<Product>(`${API_URL}/api/v1/products/${id}`, 'Product');
}

export async function fetchIndividualPlants(
  filters: PlantFilters = {},
  page = 0,
): Promise<IndividualPlantPage> {
  const params = new URLSearchParams({ status: 'AVAILABLE', size: '50', page: String(page) });
  if (filters.variegated !== undefined) {
    params.set('variegated', String(filters.variegated));
  }
  if (filters.light) {
    params.set('light', filters.light);
  }
  if (filters.maxPriceMinor !== undefined) {
    params.set('maxPriceMinor', String(filters.maxPriceMinor));
  }
  return fetchJson<IndividualPlantPage>(
    `${API_URL}/api/v1/individual-plants?${params}`,
    'Individual plants',
  );
}

export async function fetchAllIndividualPlants(): Promise<IndividualPlant[]> {
  return collectPages((page) => fetchIndividualPlants({}, page));
}

async function collectPages<T>(
  fetchPage: (page: number) => Promise<{ items: T[]; totalPages: number }>,
): Promise<T[]> {
  const first = await fetchPage(0);
  if (first.totalPages <= 1) {
    return first.items;
  }
  const rest = await Promise.all(
    Array.from({ length: first.totalPages - 1 }, (_, index) => fetchPage(index + 1)),
  );
  return [...first.items, ...rest.flatMap((page) => page.items)];
}

export async function fetchIndividualPlant(id: string): Promise<IndividualPlant> {
  return fetchJson<IndividualPlant>(
    `${API_URL}/api/v1/individual-plants/${id}`,
    'Individual plant',
  );
}

export async function fetchVariants(productId: string): Promise<Variant[]> {
  return fetchJson<Variant[]>(`${API_URL}/api/v1/products/${productId}/variants`, 'Variants');
}

export async function fetchShippingZones(): Promise<ShippingZone[]> {
  return fetchJson<ShippingZone[]>(`${API_URL}/api/v1/shipping/zones`, 'Shipping zones');
}

const PRODUCT_IMAGES: Record<string, string> = {
  'MON-001': '/images/plants/monstera.jpg',
  'MON-002': '/images/plants/monstera.jpg',
  'FERN-001': '/images/plants/fern.jpg',
  'SUCC-001': '/images/plants/echeveria.jpg',
  'SUCC-002': '/images/plants/haworthia.jpg',
  'PLANT-001': '/images/plants/philodendron.jpg',
  'PLANT-002': '/images/plants/anthurium.jpg',
  'PLANT-003': '/images/plants/alocasia.jpg',
  'PLANT-004': '/images/plants/syngonium.jpg',
  'PLANT-005': '/images/plants/scindapsus.jpg',
  'PLANT-006': '/images/plants/anthurium.jpg',
};

export function productImage(sku: string): string {
  return PRODUCT_IMAGES[sku] ?? '/images/plants/monstera.jpg';
}

export function formatPrice(priceMinor: number, currency: string): string {
  // Deterministic formatting: Node and Chromium ship different ICU data for be-BY.
  const [whole, cents] = (priceMinor / 100).toFixed(2).split('.');
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  return `${grouped},${cents} ${currency === 'BYN' ? 'Br' : currency}`;
}
