import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from './features/auth/store/authStore'
import { SESSION_EXPIRED_EVENT } from './lib/apiClient'
import App from './App'

vi.mock('./features/maps/components/MapView', () => ({
  MapView: () => <section aria-label="Map of Romania" />,
}))

describe('App', () => {
  afterEach(() => useAuthStore.getState().clear())

  it('renders the layout with an empty map (MSW is running and rejects unhandled requests)', () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter>
          <App />
        </MemoryRouter>
      </QueryClientProvider>,
    )

    expect(screen.getByText('Rootine')).toBeInTheDocument()
    expect(screen.getByLabelText('Map of Romania')).toBeInTheDocument()
  })

  it('redirects to login when the session expires', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter>
          <App />
        </MemoryRouter>
      </QueryClientProvider>,
    )

    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))

    expect(await screen.findByRole('heading', { name: 'Sign in to Rootine' })).toBeInTheDocument()
  })
})
