import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev server proxies API calls to the Spring Boot backend on :8080 so the
// frontend can call relative "/api/..." paths without CORS in development.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
