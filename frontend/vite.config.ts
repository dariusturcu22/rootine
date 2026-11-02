import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      'leaflet-draw/dist/leaflet.draw.css': fileURLToPath(
        new URL('./node_modules/leaflet-draw/dist/leaflet.draw.css', import.meta.url),
      ),
      'leaflet-draw/dist/leaflet.draw.js': fileURLToPath(
        new URL('./node_modules/leaflet-draw/dist/leaflet.draw.js', import.meta.url),
      ),
      'leaflet-draw': fileURLToPath(new URL('./src/lib/leafletDraw.ts', import.meta.url)),
    },
  },
  server: {
    proxy: {
      // Same behaviour as nginx in the Docker image: /api/x -> backend /x
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})
