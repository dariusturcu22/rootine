import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import App from './App'

vi.mock('./features/maps/components/MapView', () => ({
  MapView: () => <section aria-label="Map of Romania" />,
}))

describe('App', () => {
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
})
