import type { ReactNode } from 'react'
import type { DrawEvents } from 'leaflet'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MapView } from './MapView'

type MapContainerMockProps = {
  center: [number, number]
  zoom: number
  children?: ReactNode
}

type EditControlMockProps = {
  position: string
  onCreated: (event: DrawEvents.Created) => void
  draw: {
    polygon: boolean
    rectangle: boolean
    circle: boolean
    marker: boolean
    polyline: boolean
    circlemarker: boolean
  }
}

type PolygonMockProps = {
  positions: [number, number][]
}

const editControlProps = vi.fn<(props: EditControlMockProps) => void>()

function renderMapView() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MapView />
    </QueryClientProvider>,
  )
}

vi.mock('react-leaflet', () => ({
  MapContainer: ({ children, center, zoom }: MapContainerMockProps) => (
    <div data-testid="map-container" data-center={center.join(',')} data-zoom={zoom}>
      {children}
    </div>
  ),
  TileLayer: () => <div data-testid="tile-layer" />,
  FeatureGroup: ({ children }: { children?: ReactNode }) => (
    <div data-testid="feature-group">{children}</div>
  ),
  Polygon: ({ positions }: PolygonMockProps) => (
    <div data-testid="parcel-polygon" data-positions={JSON.stringify(positions)} />
  ),
}))

vi.mock('react-leaflet-draw', () => ({
  EditControl: (props: EditControlMockProps) => {
    editControlProps(props)
    return <div data-testid="edit-control" />
  },
}))

describe('MapView', () => {
  it('renders the Romania map with its configured initial view', () => {
    renderMapView()

    expect(screen.getByLabelText('Map of Romania')).toBeInTheDocument()
    expect(screen.getByTestId('map-container')).toHaveAttribute('data-center', '45.9432,24.9668')
    expect(screen.getByTestId('map-container')).toHaveAttribute('data-zoom', '6')
  })

  it('renders imagery, boundary, and polygon drawing controls', () => {
    renderMapView()

    expect(screen.getAllByTestId('tile-layer')).toHaveLength(2)
    expect(screen.getByTestId('feature-group')).toBeInTheDocument()
    expect(screen.getByTestId('edit-control')).toBeInTheDocument()

    expect(editControlProps).toHaveBeenLastCalledWith(
      expect.objectContaining({
        position: 'topright',
        draw: {
          polygon: true,
          rectangle: false,
          circle: false,
          marker: false,
          polyline: false,
          circlemarker: false,
        },
      }),
    )
  })

  it('opens the parcel form after converting polygon coordinates', async () => {
    renderMapView()

    const onCreated = editControlProps.mock.lastCall?.[0].onCreated
    if (!onCreated) {
      throw new Error('Expected EditControl to receive an onCreated handler')
    }

    onCreated({
      layer: {
        getLatLngs: () => [
          [
            { lat: 45, lng: 25 },
            { lat: 45, lng: 25.1 },
            { lat: 45.1, lng: 25 },
          ],
        ],
      },
    } as DrawEvents.Created)

    await waitFor(() => {
      expect(screen.getByText('Parcel name')).toBeInTheDocument()
    })
  })

  it('loads and renders parcels returned by MSW', async () => {
    renderMapView()

    await waitFor(() => {
      expect(screen.getAllByTestId('parcel-polygon').length).toBeGreaterThanOrEqual(2)
    })

    expect(
      screen
        .getAllByTestId('parcel-polygon')
        .some((polygon) => polygon.getAttribute('data-positions')?.includes('[46.5,24.1]')),
    ).toBe(true)
  })
})
