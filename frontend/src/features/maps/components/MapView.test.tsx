import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { MapView } from './MapView'

vi.mock('react-leaflet', () => {
  function Container({ children }: { children: ReactNode }) {
    return <div>{children}</div>
  }

  function Polygon({ children }: { children: ReactNode }) {
    return <div>{children}</div>
  }

  function Popup({ children }: { children: ReactNode }) {
    return <div>{children}</div>
  }

  function TileLayer() {
    return null
  }

  return {
    MapContainer: Container,
    Polygon,
    Popup,
    TileLayer,
  }
})

describe('MapView', () => {
  it('loads parcels and displays their names and crops', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <MapView />
      </QueryClientProvider>,
    )

    expect(await screen.findByText('North field (potato)')).toBeInTheDocument()
    expect(screen.getByText('South field (wheat)')).toBeInTheDocument()
  })
})
