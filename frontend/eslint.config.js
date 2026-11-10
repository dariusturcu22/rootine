import js from '@eslint/js'
import { defineConfig, globalIgnores } from 'eslint/config'
import prettier from 'eslint-config-prettier'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import globals from 'globals'
import tseslint from 'typescript-eslint'

const minimumIdentifierLength = 2
const maximumLinesPerFunction = 200

const lintBaselineFiles = [
  'src/features/diagnosis/components/ImageUpload.tsx',
  'src/features/maps/components/MapView.tsx',
  'src/test/msw/handlers.ts',
]

export default defineConfig([
  globalIgnores(['dist', 'coverage', 'src/types/api.ts']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2023,
      globals: globals.browser,
    },
    rules: {
      'id-length': ['error', { min: minimumIdentifierLength, exceptions: ['i', 'j', '_'] }],
      'max-lines-per-function': ['error', { max: maximumLinesPerFunction }],
    },
  },
  {
    files: lintBaselineFiles,
    rules: {
      'id-length': 'off',
      'max-lines-per-function': 'off',
    },
  },
  prettier,
])
