import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from './App'

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
