export interface Product {
  id: string;
  sku: string;
  name: string;
  slug: string;
  categoryId: string;
  priceMinor: number;
  currency: string;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  careLevel: 'EASY' | 'MEDIUM' | 'HARD';
  light: 'LOW' | 'MEDIUM' | 'BRIGHT' | 'DIRECT';
  petSafe: boolean;
}

interface ProductPage {
  items: Product[];
  totalElements: number;
}

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

interface Problem {
  title?: string;
  detail?: string;
}

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export async function login(email: string, password: string): Promise<TokenResponse> {
  const response = await fetch(`${API_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as Problem;
    throw new Error(problem.detail ?? problem.title ?? `Login failed (${response.status})`);
  }
  return (await response.json()) as TokenResponse;
}

export async function fetchProducts(accessToken: string): Promise<Product[]> {
  const response = await fetch(`${API_URL}/api/v1/products?page=0&size=100`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!response.ok) {
    throw new Error(`Products request failed (${response.status})`);
  }
  const page = (await response.json()) as ProductPage;
  return page.items;
}

export function formatPrice(priceMinor: number, currency: string): string {
  return new Intl.NumberFormat('be-BY', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(priceMinor / 100);
}
