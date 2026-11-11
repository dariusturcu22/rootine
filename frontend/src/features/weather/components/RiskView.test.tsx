import type { ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { RiskView } from './RiskView'

type MapContainerProps = {
  center: [number, number]
  zoom: number
  children?: ReactNode
}

type PolygonProps = {
  positions: [number, number][]
  children?: ReactNode
  eventHandlers: {
    click: (event: { latlng: { lat: number; lng: number } }) => void
  }
}

vi.mock('react-leaflet', () => ({
  MapContainer: ({ children, center, zoom }: MapContainerProps) => (
    <div data-testid="risk-map" data-center={center.join(',')} data-zoom={zoom}>
      {children}
    </div>
  ),
  TileLayer: () => <div data-testid="map-tile" />,
  Polygon: ({ positions, children, eventHandlers }: PolygonProps) => (
    <div
      data-testid="risk-parcel"
      data-positions={JSON.stringify(positions)}
      onClick={() => eventHandlers.click({ latlng: { lat: 46.775, lng: 23.595 } })}
    >
      {children}
    </div>
  ),
  Tooltip: ({ children }: { children?: ReactNode }) => (
    <span data-testid="risk-parcel-label">{children}</span>
  ),
  useMapEvents: () => ({}),
}))

function renderRiskView() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RiskView />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('RiskView', () => {
  it('loads risk for each parcel using the parcel risk endpoint', async () => {
    const user = userEvent.setup()
    renderRiskView()

    expect(await screen.findByRole('heading', { name: 'Field risk' })).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getAllByTestId('risk-parcel')).toHaveLength(2)
    })

    await waitFor(() => {
      expect(screen.getAllByText('Moderate risk').length).toBeGreaterThan(0)
      expect(screen.getAllByText('Not modelled').length).toBeGreaterThan(0)
    })
    expect(screen.getByLabelText('Risk level legend')).toBeInTheDocument()
    await user.click(screen.getAllByTestId('risk-parcel')[1]!)
    expect(screen.getByText('Disease risks')).toBeInTheDocument()
    expect(screen.getByText('late blight')).toBeInTheDocument()
  })

  it('shows risk details for a selected parcel', async () => {
    const user = userEvent.setup()
    renderRiskView()

    await waitFor(() => {
      expect(screen.getAllByTestId('risk-parcel')).toHaveLength(2)
    })
    await user.click(screen.getAllByTestId('risk-parcel')[1]!)

    const selectedPanel = within(
      screen.getByRole('button', { name: 'Close field risk details' }).parentElement!,
    )
    expect(selectedPanel.getByText('North field')).toBeInTheDocument()
    expect(selectedPanel.getByText('potato')).toBeInTheDocument()
    expect(selectedPanel.getAllByText('Moderate risk')).toHaveLength(2)
    expect(selectedPanel.getByText('42/100')).toBeInTheDocument()
    expect(
      selectedPanel.getByText(
        'Mock response: fixed medium risk, real weather rules are not implemented yet.',
      ),
    ).toBeInTheDocument()
    expect(selectedPanel.getByText('late blight')).toBeInTheDocument()
    expect(selectedPanel.getByText('hutton model')).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'Risk index' })).toHaveAttribute(
      'aria-valuenow',
      '42',
    )
  })
})
