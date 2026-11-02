import 'leaflet/dist/leaflet.css'
import 'leaflet-draw/dist/leaflet.draw.css'
import type {} from 'leaflet-draw'
import { useQuery } from '@tanstack/react-query'
import type { DrawEvents, LatLng, Polygon as LeafletPolygon } from 'leaflet'
import { EditControl } from 'react-leaflet-draw'
import { FeatureGroup, MapContainer, Polygon, Popup, TileLayer } from 'react-leaflet'
import { MAP_CENTER, MAP_ZOOM } from '../../../config/env'
import { getParcels } from '../../../lib/apiClient'

export function MapView() {
  const { data: parcels = [] } = useQuery({
    queryKey: ['parcels'],
    queryFn: getParcels,
  })

  const handlePolygonCreated = (e: DrawEvents.Created) => {
    const layer = e.layer as LeafletPolygon
    const latLngs: LatLng[] = layer.getLatLngs()[0] as LatLng[]

    //leaflet gives (lat, lng), backend needs (lng, lat)
    const backendCoords = latLngs.map((coord) => [coord.lng, coord.lat])

    console.log('Backend coordinates: ', backendCoords)
  }

  return (
    <section
      aria-label="Map of Romania"
      style={{ position: 'relative', height: '100%', width: '100%', minHeight: '400px' }}
    >
      <MapContainer
        center={MAP_CENTER}
        zoom={MAP_ZOOM}
        style={{
          position: 'absolute',
          top: 0,
          bottom: 0,
          left: 0,
          right: 0,
          height: '100%',
          width: '100%',
        }}
      >
        <TileLayer
          attribution="Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"
          url="https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
          maxZoom={19}
        />

        <TileLayer
          url="https://services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"
          maxZoom={19}
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

        <FeatureGroup>
          <EditControl
            position="topright"
            onCreated={handlePolygonCreated}
            draw={{
              polygon: true,
              rectangle: false,
              circle: false,
              marker: false,
              polyline: false,
              circlemarker: false,
            }}
          />
        </FeatureGroup>
      </MapContainer>
    </section>
  )
}
