import { QueryClientProvider } from '@tanstack/react-query'
import * as Leaflet from 'leaflet'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import './index.css'
import { queryClient } from './lib/queryClient'

async function startApp() {
  const globalWithLeaflet = globalThis as typeof globalThis & { L: typeof Leaflet }
  globalWithLeaflet.L = { ...Leaflet }
  await import('leaflet-draw')
  const { default: App } = await import('./App')

  if (import.meta.env.DEV) {
    const { worker } = await import('./test/msw/browser')
    await worker.start({ onUnhandledFrame: 'bypass' })
  }

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </QueryClientProvider>
    </StrictMode>,
  )
}

void startApp()
