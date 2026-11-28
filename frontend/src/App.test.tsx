import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
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

  function renderApp(initialPath = '/') {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={[initialPath]}>
          <App />
        </MemoryRouter>
      </QueryClientProvider>,
    )
  }

  it('starts unauthenticated visitors on the login page', () => {
    renderApp()

    expect(screen.getByRole('heading', { name: 'Sign in to Rootine' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Map of Romania')).not.toBeInTheDocument()
  })

  it('redirects protected deep links to login when unauthenticated', () => {
    renderApp('/risk')

    expect(screen.getByRole('heading', { name: 'Sign in to Rootine' })).toBeInTheDocument()
  })

  it('renders the layout after a token is present', () => {
    useAuthStore.getState().setToken('valid-access-token')
    renderApp()

    expect(screen.getByText('Rootine')).toBeInTheDocument()
    expect(screen.getByLabelText('Map of Romania')).toBeInTheDocument()
  })

  it('clears the token and returns to login when signing out', async () => {
    useAuthStore.getState().setToken('valid-access-token')
    renderApp()

    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('heading', { name: 'Sign in to Rootine' })).toBeInTheDocument()
    expect(useAuthStore.getState().token).toBeNull()
  })

  it('redirects to login when the session expires', async () => {
    useAuthStore.getState().setToken('expired-access-token')
    renderApp()

    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))

    expect(await screen.findByRole('heading', { name: 'Sign in to Rootine' })).toBeInTheDocument()
  })
})
