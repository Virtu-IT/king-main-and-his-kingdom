import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// npm start: the dev server on http://localhost:5173 sends /api to the Spring Boot application on :8080,
// so the browser sees one origin and no CORS is needed during development.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: { '/api': 'http://localhost:8080' },
  },
});
