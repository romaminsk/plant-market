export interface Category {
  id: string;
  name: string;
  slug: string;
  description: string | null;
}

export interface Product {
  id: string;
  sku: string;
  name: string;
  slug: string;
  description: string | null;
  categoryId: string;
  priceMinor: number;
  currency: string;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  careLevel: 'EASY' | 'MEDIUM' | 'HARD';
  light: 'LOW' | 'MEDIUM' | 'BRIGHT' | 'DIRECT';
  petSafe: boolean;
  variegated: boolean;
  size: string | null;
  careGuide: string | null;
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
  currency: string;
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
}

export type ClaimStatus = 'OPEN' | 'REPLACEMENT' | 'REFUND' | 'REJECTED';
export type ClaimDecision = 'REPLACEMENT' | 'REFUND' | 'REJECT';

export interface Claim {
  id: string;
  orderId: string;
  customerId: string;
  status: ClaimStatus;
  reason: string;
  photoUrls: string[];
  decisionComment: string | null;
  createdAt: string;
  updatedAt: string;
  decidedAt: string | null;
}

export interface StockBatch {
  id: string;
  productVariantId: string;
  supplier: string;
  quantity: number;
  remainingQuantity: number;
  unitCostMinor: number;
  currency: string;
  receivedAt: string;
  quarantineUntil: string;
  status: 'QUARANTINE' | 'AVAILABLE' | 'DEPLETED';
}

interface AdminUser {
  id: string;
  email: string;
  fullName: string;
  roles: string[];
}

interface Problem {
  title?: string;
  detail?: string;
}

type JsonBody = Record<string, unknown>;

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
const AUTH_URL = API_URL;

export async function login(email: string, password: string): Promise<AdminUser> {
  const response = await fetch(`${AUTH_URL}/api/v1/auth/session`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', 'X-Admin-Request': '1' },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as Problem;
    throw new Error(problem.detail ?? problem.title ?? `Login failed (${response.status})`);
  }
  return (await response.json()) as AdminUser;
}

export async function checkSession(): Promise<boolean> {
  const response = await fetch(`${API_URL}/api/v1/auth/me`, {
    credentials: 'include',
    headers: { 'X-Admin-Request': '1' },
  });
  if (response.ok) {
    return true;
  }
  if (response.status !== 401) {
    return false;
  }
  return refreshSession();
}

export async function refreshSession(): Promise<boolean> {
  const response = await fetch(`${AUTH_URL}/api/v1/auth/session/refresh`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'X-Admin-Request': '1' },
  });
  return response.ok;
}

export async function logout(): Promise<void> {
  await fetch(`${AUTH_URL}/api/v1/auth/session/logout`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'X-Admin-Request': '1' },
  });
}

async function request<T>(
  path: string,
  _accessToken: string,
  init: RequestInit = {},
  retry = true,
): Promise<T> {
  const headers: Record<string, string> = { 'X-Admin-Request': '1' };
  if (init.body && !(init.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }
  const response = await fetch(`${API_URL}${path}`, {
    ...init,
    credentials: 'include',
    headers: { ...headers, ...(init.headers as Record<string, string> | undefined) },
  });
  if (response.status === 401 && retry && (await refreshSession())) {
    return request<T>(path, _accessToken, init, false);
  }
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as Problem;
    throw new Error(problem.detail ?? problem.title ?? `Request failed (${response.status})`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export async function fetchProducts(accessToken: string): Promise<Product[]> {
  return fetchAllPages<Product>('/api/v1/products', accessToken);
}

async function fetchAllPages<T>(path: string, accessToken: string): Promise<T[]> {
  const items: T[] = [];
  let page = 0;
  for (;;) {
    const response = await request<{ items: T[]; totalElements: number }>(
      `${path}?page=${page}&size=100`,
      accessToken,
    );
    items.push(...response.items);
    if (items.length >= response.totalElements || response.items.length === 0) {
      return items;
    }
    page += 1;
  }
}

export async function fetchCategories(accessToken: string): Promise<Category[]> {
  return request<Category[]>('/api/v1/categories', accessToken);
}

export async function createCategory(accessToken: string, body: JsonBody): Promise<Category> {
  return request<Category>('/api/v1/categories', accessToken, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function updateCategory(
  accessToken: string,
  categoryId: string,
  body: JsonBody,
): Promise<Category> {
  return request<Category>(`/api/v1/categories/${categoryId}`, accessToken, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export async function deleteCategory(accessToken: string, categoryId: string): Promise<void> {
  await request<void>(`/api/v1/categories/${categoryId}`, accessToken, { method: 'DELETE' });
}

export async function archiveProduct(accessToken: string, productId: string): Promise<void> {
  await request<void>(`/api/v1/products/${productId}`, accessToken, { method: 'DELETE' });
}

export async function createProduct(accessToken: string, body: JsonBody): Promise<Product> {
  return request<Product>('/api/v1/products', accessToken, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function updateProduct(
  accessToken: string,
  productId: string,
  body: JsonBody,
): Promise<Product> {
  return request<Product>(`/api/v1/products/${productId}`, accessToken, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export async function fetchVariants(accessToken: string, productId: string): Promise<Variant[]> {
  return request<Variant[]>(`/api/v1/products/${productId}/variants`, accessToken);
}

export async function createVariant(
  accessToken: string,
  productId: string,
  body: JsonBody,
): Promise<Variant> {
  return request<Variant>(`/api/v1/products/${productId}/variants`, accessToken, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function updateVariant(
  accessToken: string,
  variantId: string,
  body: JsonBody,
): Promise<Variant> {
  return request<Variant>(`/api/v1/variants/${variantId}`, accessToken, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export async function archiveVariant(accessToken: string, variantId: string): Promise<void> {
  await request<void>(`/api/v1/variants/${variantId}`, accessToken, { method: 'DELETE' });
}

export async function fetchIndividualPlants(accessToken: string): Promise<IndividualPlant[]> {
  return fetchAllPages<IndividualPlant>('/api/v1/individual-plants', accessToken);
}

export async function createIndividualPlant(
  accessToken: string,
  productId: string,
  body: JsonBody,
): Promise<IndividualPlant> {
  return request<IndividualPlant>(`/api/v1/products/${productId}/individual-plants`, accessToken, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function updateIndividualPlant(
  accessToken: string,
  plantId: string,
  body: JsonBody,
): Promise<IndividualPlant> {
  return request<IndividualPlant>(`/api/v1/individual-plants/${plantId}`, accessToken, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export async function updatePlantPhotos(
  accessToken: string,
  plantId: string,
  photos: PlantPhoto[],
): Promise<IndividualPlant> {
  return request<IndividualPlant>(`/api/v1/individual-plants/${plantId}/photos`, accessToken, {
    method: 'PUT',
    body: JSON.stringify({ photos }),
  });
}

export async function uploadMedia(_accessToken: string, file: File): Promise<string> {
  const form = new FormData();
  form.append('file', file);
  const response = await fetch(`${API_URL}/api/v1/media`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'X-Admin-Request': '1' },
    body: form,
  });
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as Problem;
    throw new Error(problem.detail ?? problem.title ?? `Upload failed (${response.status})`);
  }
  return ((await response.json()) as { url: string }).url;
}

export async function transitionPlant(
  accessToken: string,
  plantId: string,
  status: string,
): Promise<IndividualPlant> {
  return request<IndividualPlant>(`/api/v1/individual-plants/${plantId}/transitions`, accessToken, {
    method: 'POST',
    body: JSON.stringify({ status }),
  });
}

export async function fetchClaims(accessToken: string): Promise<Claim[]> {
  return request<Claim[]>('/api/v1/claims', accessToken);
}

export async function decideClaim(
  accessToken: string,
  claimId: string,
  decision: ClaimDecision,
  comment: string | null,
): Promise<Claim> {
  return request<Claim>(`/api/v1/claims/${claimId}/decision`, accessToken, {
    method: 'POST',
    body: JSON.stringify({ decision, comment }),
  });
}

export async function fetchStockBatches(accessToken: string): Promise<StockBatch[]> {
  return request<StockBatch[]>('/api/v1/stock-batches', accessToken);
}

export async function createStockBatch(accessToken: string, body: JsonBody): Promise<StockBatch> {
  return request<StockBatch>('/api/v1/stock-batches', accessToken, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function releaseStockBatch(accessToken: string, batchId: string): Promise<StockBatch> {
  return request<StockBatch>(`/api/v1/stock-batches/${batchId}/release`, accessToken, {
    method: 'POST',
  });
}

export function formatPrice(priceMinor: number, currency: string): string {
  // Keep admin prices identical to the storefront regardless of browser ICU data.
  const [whole, cents] = (priceMinor / 100).toFixed(2).split('.');
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  return `${grouped},${cents} ${currency === 'BYN' ? 'Br' : currency}`;
}
