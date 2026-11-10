import type { ReactNode } from 'react'
import type { DrawEvents } from 'leaflet'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { deleteParcel } from '../../../lib/apiClient'
import { MapView } from './MapView'

vi.mock('../../../lib/apiClient', async () => {
  const actual =
    await vi.importActual<typeof import('../../../lib/apiClient')>('../../../lib/apiClient')

  return {
    ...actual,
    deleteParcel: vi.fn(),
  }
})

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
  children?: ReactNode
  eventHandlers?: {
    click: (event: { latlng: { lat: number; lng: number } }) => void
  }
}

const editControlProps = vi.fn<(props: EditControlMockProps) => void>()
const registeredMapEvents: Array<{
  zoomend: (event: { target: { getZoom: () => number } }) => void
}> = []

function renderMapView() {
  registeredMapEvents.length = 0
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <MapView />
      </MemoryRouter>
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
  Polygon: ({ positions, children, eventHandlers }: PolygonMockProps) => (
    <div
      data-testid="parcel-polygon"
      data-positions={JSON.stringify(positions)}
      onClick={() => eventHandlers?.click({ latlng: { lat: 46.7, lng: 24.1 } })}
    >
      {children}
    </div>
  ),
  Popup: ({ children }: { children?: ReactNode }) => (
    <div data-testid="parcel-popup">{children}</div>
  ),
  Tooltip: ({ children }: { children?: ReactNode }) => (
    <span data-testid="parcel-label">{children}</span>
  ),
  useMapEvents: (handlers: (typeof registeredMapEvents)[number]) => {
    registeredMapEvents.push(handlers)
    return {}
  },
}))

vi.mock('react-leaflet-draw', () => ({
  EditControl: (props: EditControlMockProps) => {
    editControlProps(props)
    return <div data-testid="edit-control" />
  },
}))

describe('MapView', () => {
  beforeEach(() => {
    registeredMapEvents.length = 0
  })

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

  it('shows parcel labels and opens its details with a diagnosis link when clicked', async () => {
    renderMapView()

    await waitFor(() => {
      expect(screen.getAllByTestId('parcel-polygon')).toHaveLength(2)
    })

    expect(screen.queryByTestId('parcel-label')).not.toBeInTheDocument()

    registeredMapEvents.at(-1)?.zoomend({ target: { getZoom: () => 9 } })

    await waitFor(() => {
      expect(screen.getAllByTestId('parcel-label')).toHaveLength(2)
    })

    expect(screen.getAllByTestId('parcel-label').map((label) => label.textContent)).toEqual([
      'South field',
      'North field',
    ])

    await userEvent.click(screen.getAllByTestId('parcel-polygon')[0]!)

    expect(screen.getByTestId('parcel-popup')).toHaveTextContent('South field')
    expect(screen.getByTestId('parcel-popup')).toHaveTextContent('tomato')
    expect(screen.getByRole('link', { name: 'Diagnose a leaf' })).toHaveAttribute(
      'href',
      '/diagnosis?parcelId=7c9e6679-7425-40de-944b-e07fc1f90ae7',
    )
  })

  it('asks for confirmation before deleting a parcel instead of showing its details', async () => {
    const user = userEvent.setup()
    vi.mocked(deleteParcel).mockResolvedValue()
    renderMapView()

    await waitFor(() => {
      expect(screen.getAllByTestId('parcel-polygon')).toHaveLength(2)
    })

    await user.click(screen.getByRole('button', { name: 'Delete parcel mode' }))
    await user.click(screen.getAllByTestId('parcel-polygon')[0]!)

    expect(screen.getByText('Are you sure you want to delete this parcel?')).toBeInTheDocument()
    expect(screen.queryByText('Crop')).not.toBeInTheDocument()
    expect(deleteParcel).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: /^Delete parcel$/ }))

    await waitFor(() => {
      expect(vi.mocked(deleteParcel).mock.calls[0]?.[0]).toBe(
        '7c9e6679-7425-40de-944b-e07fc1f90ae7',
      )
    })
  })
})
