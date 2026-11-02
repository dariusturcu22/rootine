import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  resolve: {
    alias: [{ find: /^leaflet$/, replacement: 'leaflet/dist/leaflet-src.esm.js' }],
    dedupe: ['react', 'react-dom', 'react-leaflet', '@react-leaflet/core'],
  },
  optimizeDeps: {
    exclude: ['react-leaflet-draw'],
    include: ['@react-leaflet/core', 'fast-deep-equal', 'prop-types'],
  },
  plugins: [
    react(),
    {
      name: 'fix-react-leaflet-draw',
      enforce: 'pre',
      transform(code, id) {
        if (
          id.includes('react-leaflet-draw') &&
          (code.includes("import Draw from 'leaflet-draw';") ||
            code.includes("import { PropTypes } from 'prop-types';") ||
            code.includes("import isEqual from 'fast-deep-equal';") ||
            code.includes("import leaflet, { Map, Control } from 'leaflet';"))
        ) {
          return code
            .replace("import Draw from 'leaflet-draw';", "const Draw = undefined;")
            .replace(
              "import { PropTypes } from 'prop-types';",
              "import * as PropTypes from 'prop-types';",
            )
            .replace(
              "import isEqual from 'fast-deep-equal';",
              "import * as isEqualModule from 'fast-deep-equal';\nconst isEqual = isEqualModule.default ?? isEqualModule;",
            )
            .replace(
              "import leaflet, { Map, Control } from 'leaflet';",
              "const leaflet = globalThis.L;\nconst { Map, Control } = leaflet;",
            )
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
