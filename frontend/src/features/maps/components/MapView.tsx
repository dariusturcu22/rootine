import 'leaflet/dist/leaflet.css'
import { useQuery } from '@tanstack/react-query'
import { MapContainer, Polygon, Popup, TileLayer } from 'react-leaflet'
import { MAP_CENTER, MAP_ZOOM } from '../../../config/env'
import { getParcels } from '../../../lib/apiClient'

export function MapView() {
  const { data: parcels = [] } = useQuery({
    queryKey: ['parcels'],
    queryFn: getParcels,
  })

  return (
    <section aria-label="Map of Romania" style={{ height: '100%', width: '100%' }}>
      <MapContainer center={MAP_CENTER} zoom={MAP_ZOOM} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {parcels.map((parcel) => (
          <Polygon
            key={parcel.id}
            positions={parcel.geometry.coordinates[0].map(([longitude, latitude]) => [
              latitude,
              longitude,
            ])}
          >
            <Popup>
              {parcel.name} ({parcel.crop})
            </Popup>
          </Polygon>
        ))}
      </MapContainer>
    </section>
  )
}
