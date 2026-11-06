export type CartItemType = 'INDIVIDUAL_PLANT' | 'VARIANT';

export interface CartItem {
  type: CartItemType;
  id: string;
  name: string;
  priceMinor: number;
  currency: string;
  quantity: number;
}

const KEY = 'pm.cart.v1';
const EVENT = 'pm-cart-changed';

function isBrowser(): boolean {
  return typeof window !== 'undefined';
}

export function readCart(): CartItem[] {
  if (!isBrowser()) {
    return [];
  }
  try {
    const parsed = JSON.parse(window.localStorage.getItem(KEY) ?? '[]') as CartItem[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function writeCart(items: CartItem[]): void {
  if (!isBrowser()) {
    return;
  }
  window.localStorage.setItem(KEY, JSON.stringify(items));
  window.dispatchEvent(new Event(EVENT));
}

export function addToCart(item: CartItem): void {
  const items = readCart();
  const existing = items.find((entry) => entry.id === item.id && entry.type === item.type);
  if (existing) {
    if (item.type === 'VARIANT') {
      existing.quantity += item.quantity;
    }
  } else {
    items.push(item);
  }
  writeCart(items);
}

export function removeFromCart(type: CartItemType, id: string): void {
  writeCart(readCart().filter((item) => !(item.type === type && item.id === id)));
}

export function clearCart(): void {
  writeCart([]);
}

export function cartCount(): number {
  return readCart().reduce((total, item) => total + item.quantity, 0);
}

export function cartTotal(): number {
  return readCart().reduce((total, item) => total + item.priceMinor * item.quantity, 0);
}

export function subscribeToCart(listener: () => void): () => void {
  if (!isBrowser()) {
    return () => {};
  }
  window.addEventListener(EVENT, listener);
  window.addEventListener('storage', listener);
  return () => {
    window.removeEventListener(EVENT, listener);
    window.removeEventListener('storage', listener);
  };
}
