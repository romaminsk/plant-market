const FALLBACK = '/checkout';

function hasControlCharacters(value: string): boolean {
  for (const character of value) {
    const code = character.codePointAt(0) ?? 0;
    if (code < 32 || code === 127) {
      return true;
    }
  }
  return false;
}

/**
 * Accepts only same-origin absolute paths. Backslashes, encoded backslashes and
 * protocol-relative URLs are rejected before the URL parser can normalize them.
 */
export function safeReturnTo(value: string | null | undefined, fallback = FALLBACK): string {
  if (!value || !value.startsWith('/') || value.startsWith('//')) {
    return fallback;
  }
  if (value.includes('\\') || hasControlCharacters(value)) {
    return fallback;
  }
  let decoded: string;
  try {
    decoded = decodeURIComponent(value);
  } catch {
    return fallback;
  }
  if (decoded.includes('\\') || decoded.startsWith('//')) {
    return fallback;
  }
  try {
    const url = new URL(value, 'http://localhost');
    if (url.origin !== 'http://localhost') {
      return fallback;
    }
    return url.pathname + url.search + url.hash;
  } catch {
    return fallback;
  }
}
