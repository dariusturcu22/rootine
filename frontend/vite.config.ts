import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    {
      name: 'fix-react-leaflet-draw',
      enforce: 'pre',
      transform(code, id) {
        if (
          id.includes('react-leaflet-draw') &&
          code.includes("import Draw from 'leaflet-draw';")
        ) {
          return code.replace("import Draw from 'leaflet-draw';", "import 'leaflet-draw';")
        }
      },
    },
  ],
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
