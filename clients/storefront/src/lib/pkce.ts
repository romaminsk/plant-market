import { createHash, randomBytes } from 'node:crypto';

export function createVerifier(): string {
  return randomBytes(48).toString('base64url');
}

export function createChallenge(verifier: string): string {
  return createHash('sha256').update(verifier).digest('base64url');
}
