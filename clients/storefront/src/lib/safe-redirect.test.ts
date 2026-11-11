import assert from 'node:assert/strict';
import test from 'node:test';
import { safeReturnTo } from './safe-redirect.ts';

test('keeps a normal internal path', () => {
  assert.equal(safeReturnTo('/checkout'), '/checkout');
  assert.equal(safeReturnTo('/catalog?light=BRIGHT#top'), '/catalog?light=BRIGHT#top');
});

test('falls back for an absolute external URL', () => {
  assert.equal(safeReturnTo('https://evil.example/phish'), '/checkout');
});

test('falls back for a protocol-relative URL', () => {
  assert.equal(safeReturnTo('//evil.example/phish'), '/checkout');
});

test('falls back for a backslash trick', () => {
  assert.equal(safeReturnTo('/\\evil.example/phish'), '/checkout');
});

test('falls back for an encoded backslash trick', () => {
  assert.equal(safeReturnTo('/%5Cevil.example/phish'), '/checkout');
});

test('falls back for encoded protocol-relative URL', () => {
  assert.equal(safeReturnTo('/%2F%2Fevil.example/phish'), '/checkout');
});

test('falls back for dot-segment protocol-relative trick', () => {
  assert.equal(safeReturnTo('/..//evil.example/phish'), '/checkout');
});

test('falls back for empty and missing values', () => {
  assert.equal(safeReturnTo(null), '/checkout');
  assert.equal(safeReturnTo(''), '/checkout');
});
