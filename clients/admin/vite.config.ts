import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const apiUrl = process.env.VITE_API_URL ?? 'http://localhost:8080';
const apiWsUrl = apiUrl.replace(/^http/, 'ws');
const securityHeaders = {
  'X-Frame-Options': 'DENY',
  'X-Content-Type-Options': 'nosniff',
  'Referrer-Policy': 'no-referrer',
  'Permissions-Policy': 'geolocation=(), microphone=(), camera=()',
  'Content-Security-Policy': `default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' https: data:; connect-src 'self' ${apiUrl} ${apiWsUrl} ws://localhost:5173; frame-ancestors 'none'; base-uri 'self'`,
};

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    headers: securityHeaders,
  },
  preview: {
    port: 5173,
    headers: securityHeaders,
  },
});
