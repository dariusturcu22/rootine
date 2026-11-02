import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MapView } from './MapView'

const editControlProps = vi.fn()

vi.mock('react-leaflet', () => ({
  MapContainer: ({ children, center, zoom }: any) => (
    <div data-testid="map-container" data-center={center.join(',')} data-zoom={zoom}>
      {children}
    </div>
  ),
  TileLayer: () => <div data-testid="tile-layer" />,
  FeatureGroup: ({ children }: any) => (
    <div data-testid="feature-group">{children}</div>
  ),
}))

vi.mock('react-leaflet-draw', () => ({
  EditControl: (props: any) => {
    editControlProps(props)
    return <div data-testid="edit-control" />
  },
}))

describe('MapView', () => {
  it('renders the Romania map with its configured initial view', () => {
    render(<MapView />)

    expect(screen.getByLabelText('Map of Romania')).toBeInTheDocument()
    expect(screen.getByTestId('map-container')).toHaveAttribute(
      'data-center',
      '45.9432,24.9668',
    )
    expect(screen.getByTestId('map-container')).toHaveAttribute('data-zoom', '6')
  })

  it('renders imagery, boundary, and polygon drawing controls', () => {
    render(<MapView />)

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

  it('converts created polygon coordinates to backend longitude-latitude order', () => {
    const consoleLog = vi.spyOn(console, 'log').mockImplementation(() => {})

    render(<MapView />)

    const onCreated = editControlProps.mock.lastCall?.[0].onCreated
    onCreated({
      layer: {
        getLatLngs: () => [
          [
            { lat: 46.1, lng: 24.5 },
            { lat: 46.2, lng: 24.6 },
          ],
        ],
      },
    })

    expect(consoleLog).toHaveBeenCalledWith('Backend coordinates: ', [
      [24.5, 46.1],
      [24.6, 46.2],
    ])

    consoleLog.mockRestore()
  })
})
