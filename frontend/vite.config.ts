import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The app calls the API gateway directly (VITE_API_BASE_URL, default http://localhost:8080).
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
});
