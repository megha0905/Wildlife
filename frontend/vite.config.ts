import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Locally the app calls the gateway at VITE_API_BASE_URL (default http://localhost:8080).
// In GitHub Codespaces, frontend/.env sets VITE_API_BASE_URL empty: the app then calls /api on its
// own address and this proxy forwards it to the gateway, so only port 5173 needs to be opened.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    host: true,
    allowedHosts: true,
    proxy: { '/api': 'http://localhost:8080' },
  },
});
