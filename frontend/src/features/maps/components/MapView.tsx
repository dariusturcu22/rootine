import 'leaflet/dist/leaflet.css'
import { MapContainer, TileLayer } from 'react-leaflet'
import { MAP_CENTER, MAP_ZOOM } from '../../../config/env'

export function MapView() {
  return (
    <section aria-label="Map of Romania" style={{ height: '100%', width: '100%' }}>
      <MapContainer center={MAP_CENTER} zoom={MAP_ZOOM} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
      </MapContainer>
    </section>
  )
}
