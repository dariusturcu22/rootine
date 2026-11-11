import { QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import App from './App'
import './index.css'
import { getDemoToken } from './lib/apiClient'
import { queryClient } from './lib/queryClient'
import { useAuthStore } from './features/auth/store/authStore'

async function start() {
  const root = createRoot(document.getElementById('root')!)

  try {
    if (import.meta.env.DEV && import.meta.env.VITE_USE_MSW === 'true') {
      const { worker } = await import('./test/msw/browser')
      await worker.start()
    }

    if (import.meta.env.VITE_DEMO_MODE === 'true') {
      const { accessToken } = await getDemoToken()
      useAuthStore.getState().setToken(accessToken)
    }

    root.render(
      <StrictMode>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </QueryClientProvider>
      </StrictMode>,
    )
  } catch (error) {
    root.render(
      <p role="alert">
        Unable to initialize the app: {error instanceof Error ? error.message : 'Unknown error'}
      </p>,
    )
  }
}

void start()
