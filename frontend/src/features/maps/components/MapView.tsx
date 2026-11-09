import 'leaflet/dist/leaflet.css'
import 'leaflet-draw/dist/leaflet.draw.css'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { DrawEvents, LatLng, Polygon as LeafletPolygon, Layer } from 'leaflet'
import { useState, useRef, useEffect, useCallback } from 'react'
import {
  FeatureGroup,
  MapContainer,
  Polygon,
  Popup,
  TileLayer,
  Tooltip,
  useMapEvents,
} from 'react-leaflet'
import { EditControl } from 'react-leaflet-draw'
import { Link } from 'react-router'
import { MAP_CENTER, MAP_ZOOM } from '../../../config/env'
import {
  createParcel,
  deleteParcel,
  getCrops,
  getParcels,
  updateParcel,
  type ParcelGeometry,
  type Position,
  type ParcelInput,
} from '../../../lib/apiClient'
import './MapView.css'

type ParcelLayer = LeafletPolygon & { parcelId?: string }

function ParcelZoomListener({ onZoomChange }: { onZoomChange: (zoom: number) => void }) {
  useMapEvents({
    zoomend: (event) => onZoomChange(event.target.getZoom()),
  })

  return null
}

function orientation(a: Position, b: Position, c: Position): number {
  return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
}

function isPointOnSegment(point: Position, start: Position, end: Position): boolean {
  return (
    Math.min(start[0], end[0]) <= point[0] &&
    point[0] <= Math.max(start[0], end[0]) &&
    Math.min(start[1], end[1]) <= point[1] &&
    point[1] <= Math.max(start[1], end[1]) &&
    orientation(start, end, point) === 0
  )
}

function segmentsIntersect(
  firstStart: Position,
  firstEnd: Position,
  secondStart: Position,
  secondEnd: Position,
): boolean {
  const firstOrientation = orientation(firstStart, firstEnd, secondStart)
  const secondOrientation = orientation(firstStart, firstEnd, secondEnd)
  const thirdOrientation = orientation(secondStart, secondEnd, firstStart)
  const fourthOrientation = orientation(secondStart, secondEnd, firstEnd)

  if (
    ((firstOrientation > 0 && secondOrientation < 0) ||
      (firstOrientation < 0 && secondOrientation > 0)) &&
    ((thirdOrientation > 0 && fourthOrientation < 0) ||
      (thirdOrientation < 0 && fourthOrientation > 0))
  ) {
    return true
  }

  return (
    (firstOrientation === 0 && isPointOnSegment(secondStart, firstStart, firstEnd)) ||
    (secondOrientation === 0 && isPointOnSegment(secondEnd, firstStart, firstEnd)) ||
    (thirdOrientation === 0 && isPointOnSegment(firstStart, secondStart, secondEnd)) ||
    (fourthOrientation === 0 && isPointOnSegment(firstEnd, secondStart, secondEnd))
  )
}

function isPointInsidePolygon(point: Position, ring: Position[]): boolean {
  let inside = false

  for (let index = 0, previous = ring.length - 1; index < ring.length; previous = index++) {
    const currentPoint = ring[index]
    const previousPoint = ring[previous]

    if (!currentPoint || !previousPoint) {
      continue
    }

    const crossesEdge = currentPoint[1] > point[1] !== previousPoint[1] > point[1]
    if (
      crossesEdge &&
      point[0] <
        ((previousPoint[0] - currentPoint[0]) * (point[1] - currentPoint[1])) /
          (previousPoint[1] - currentPoint[1]) +
          currentPoint[0]
    ) {
      inside = !inside
    }
  }

  return inside
}

function polygonsOverlap(firstRing: Position[], secondRing: Position[]): boolean {
  for (let firstIndex = 0; firstIndex < firstRing.length - 1; firstIndex += 1) {
    const firstStart = firstRing[firstIndex]
    const firstEnd = firstRing[firstIndex + 1]

    if (!firstStart || !firstEnd) {
      continue
    }

    for (let secondIndex = 0; secondIndex < secondRing.length - 1; secondIndex += 1) {
      const secondStart = secondRing[secondIndex]
      const secondEnd = secondRing[secondIndex + 1]

      if (
        secondStart &&
        secondEnd &&
        segmentsIntersect(firstStart, firstEnd, secondStart, secondEnd)
      ) {
        return true
      }
    }
  }

  const firstPoint = firstRing[0]
  const secondPoint = secondRing[0]

  return Boolean(
    (firstPoint && isPointInsidePolygon(firstPoint, secondRing)) ||
    (secondPoint && isPointInsidePolygon(secondPoint, firstRing)),
  )
}

export function MapView() {
  const queryClient = useQueryClient()
  const [geometry, setGeometry] = useState<ParcelGeometry | null>(null)
  const [drawError, setDrawError] = useState<string | null>(null)
  const [editControlVersion, setEditControlVersion] = useState(0)
  const [mapZoom, setMapZoom] = useState(MAP_ZOOM)
  const [deleteMode, setDeleteMode] = useState(false)
  const [selectedParcelId, setSelectedParcelId] = useState<string | null>(null)
  const [selectedParcelPosition, setSelectedParcelPosition] = useState<[number, number] | null>(
    null,
  )
  const temporaryLayer = useRef<Layer | null>(null)

  const { data: parcels = [], isError: parcelsError } = useQuery({
    queryKey: ['parcels'],
    queryFn: getParcels,
  })

  const { data: crops = [], isError: cropsError } = useQuery({
    queryKey: ['crops'],
    queryFn: getCrops,
  })

  const parcelsRef = useRef(parcels)
  const parcelLayersRef = useRef(new Map<string, ParcelLayer>())

  useEffect(() => {
    parcelsRef.current = parcels
  }, [parcels])

  const registerParcelLayer = useCallback((id: string, layer: LeafletPolygon | null) => {
    if (!layer) {
      parcelLayersRef.current.delete(id)
      return
    }
    const parcelLayer = layer as ParcelLayer
    parcelLayer.parcelId = id
    parcelLayersRef.current.set(id, parcelLayer)
  }, [])

  const createParcelMutation = useMutation({
    mutationFn: createParcel,
    onSuccess: async () => {
      temporaryLayer.current?.remove()
      temporaryLayer.current = null
      setGeometry(null)
      setDrawError(null)
      await queryClient.invalidateQueries({ queryKey: ['parcels'] })
      setEditControlVersion((v) => v + 1)
    },
  })

  const updateParcelMutation = useMutation({
    mutationFn: ({ id, parcel }: { id: string; parcel: ParcelInput }) => updateParcel(id, parcel),
    onSuccess: async () => {
      setDrawError(null)
      await queryClient.invalidateQueries({ queryKey: ['parcels'] })
      setEditControlVersion((version) => version + 1)
    },
    onError: async () => {
      setDrawError('Unable to update parcel. Please try again.')
      await queryClient.invalidateQueries({ queryKey: ['parcels'] })
      setEditControlVersion((version) => version + 1)
    },
  })

  const deleteParcelMutation = useMutation({
    mutationFn: deleteParcel,
    onSuccess: async () => {
      setSelectedParcelId(null)
      setSelectedParcelPosition(null)
      setDeleteMode(false)
      await queryClient.invalidateQueries({ queryKey: ['parcels'] })
    },
  })

  const updateParcelMutationRef = useRef(updateParcelMutation)
  useEffect(() => {
    updateParcelMutationRef.current = updateParcelMutation
  }, [updateParcelMutation])

  const handlePolygonCreated = (event: DrawEvents.Created) => {
    temporaryLayer.current = event.layer
    setDrawError(null)

    const layer = event.layer as LeafletPolygon
    const latLngs = layer.getLatLngs()[0] as LatLng[]

    const coordinates: Position[] = latLngs.map(({ lng, lat }) => [lng, lat])

    const first = coordinates[0]
    const last = coordinates.at(-1)

    if (!first || !last) {
      return
    }

    if (first[0] !== last[0] || first[1] !== last[1]) {
      coordinates.push([...first])
    }

    if (coordinates.length >= 4) {
      const overlapsExistingParcel = parcelsRef.current.some((parcel) =>
        polygonsOverlap(coordinates, parcel.geometry.coordinates[0] ?? []),
      )

      if (overlapsExistingParcel) {
        temporaryLayer.current?.remove()
        temporaryLayer.current = null
        setGeometry(null)
        setDrawError('This parcel overlaps an existing parcel. Draw a different area.')
        return
      }

      setGeometry({
        type: 'Polygon',
        coordinates: [coordinates],
      })
    }
  }

  const handleParcelsEdited = useCallback((event: DrawEvents.Edited) => {
    event.layers.eachLayer((layer) => {
      const parcelLayer = layer as ParcelLayer
      const parcel = parcelsRef.current.find(
        ({ id }) => id === parcelLayer.parcelId || parcelLayersRef.current.get(id) === parcelLayer,
      )
      if (!parcel) return

      const latLngs = (parcelLayer.getLatLngs()[0] as LatLng[]).map((coordinate): Position => [
        coordinate.lng,
        coordinate.lat,
      ])
      const firstCoordinate = latLngs[0]
      const lastCoordinate = latLngs[latLngs.length - 1]

      if (
        firstCoordinate &&
        lastCoordinate &&
        (firstCoordinate[0] !== lastCoordinate[0] || firstCoordinate[1] !== lastCoordinate[1])
      ) {
        latLngs.push([...firstCoordinate])
      }

      const overlapsOtherParcel = parcelsRef.current.some(
        (otherParcel) =>
          otherParcel.id !== parcel.id &&
          polygonsOverlap(latLngs, otherParcel.geometry.coordinates[0]),
      )
      if (overlapsOtherParcel) {
        parcelLayer.setLatLngs(
          parcel.geometry.coordinates[0].map(([longitude, latitude]) => [latitude, longitude]),
        )
        setDrawError('This parcel overlaps an existing parcel. Edit it in a different area.')
        return
      }

      setDrawError(null)
      parcelLayer.setStyle({ color: '#2E7D32', fillColor: '#C5E1A5', fillOpacity: 0.55 })
      updateParcelMutationRef.current.mutate({
        id: parcel.id,
        parcel: {
          name: parcel.name,
          crop: parcel.crop,
          geometry: { type: 'Polygon', coordinates: [latLngs] },
        },
      })
    })
  }, [])

  return (
    <section className="map-page" aria-label="Map of Romania">
      <header className="page-heading map-heading">
        <div>
          <p className="eyebrow">YOUR FARM</p>
          <h1>Field map</h1>
          <p className="page-description">View your parcels and draw a boundary to add a field.</p>
        </div>
        <span className="parcel-count">
          {parcels.length} {parcels.length === 1 ? 'parcel' : 'parcels'}
        </span>
      </header>

      <div className="map-frame">
        <MapContainer center={MAP_CENTER} zoom={MAP_ZOOM} className="rootine-leaflet-map">
          <ParcelZoomListener onZoomChange={setMapZoom} />
          <TileLayer
            attribution="Tiles &copy; Esri"
            url="https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
            maxZoom={19}
          />

          <TileLayer
            url="https://services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"
            maxZoom={19}
          />

          <FeatureGroup key={editControlVersion}>
            {parcels.map((parcel) => (
              <Polygon
                key={parcel.id}
                ref={(layer) => registerParcelLayer(parcel.id, layer)}
                eventHandlers={{
                  click: (event) => {
                    setSelectedParcelId(parcel.id)
                    setSelectedParcelPosition([event.latlng.lat, event.latlng.lng])
                  },
                }}
                positions={parcel.geometry.coordinates[0].map(([longitude, latitude]) => [
                  latitude,
                  longitude,
                ])}
                pathOptions={{
                  color: '#2E7D32',
                  fillColor: '#C5E1A5',
                  fillOpacity: 0.55,
                }}
              >
                {mapZoom >= 9 && (
                  <Tooltip permanent direction="center" className="parcel-name-label">
                    {parcel.name}
                  </Tooltip>
                )}
              </Polygon>
            ))}

            <EditControl
              position="topright"
              onCreated={handlePolygonCreated}
              onEdited={handleParcelsEdited}
              edit={{ remove: false }}
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
          {selectedParcelId && selectedParcelPosition && (
            <Popup
              position={selectedParcelPosition}
              className="parcel-popup"
              eventHandlers={{
                remove: () => {
                  setSelectedParcelId(null)
                  setSelectedParcelPosition(null)
                },
              }}
            >
              {(() => {
                const selectedParcel = parcels.find(({ id }) => id === selectedParcelId)
                if (!selectedParcel) return null

                return (
                  <div className="parcel-card">
                    <span className="eyebrow">FIELD DETAILS</span>
                    <h2>{selectedParcel.name}</h2>
                    {deleteMode ? (
                      <>
                        <p className="parcel-delete-question">
                          Are you sure you want to delete this parcel?
                        </p>
                        <div className="parcel-delete-actions">
                          <button
                            className="parcel-cancel-delete"
                            type="button"
                            onClick={() => {
                              setSelectedParcelId(null)
                              setSelectedParcelPosition(null)
                              setDeleteMode(false)
                              deleteParcelMutation.reset()
                            }}
                          >
                            Cancel
                          </button>
                          <button
                            className="parcel-confirm-delete"
                            type="button"
                            disabled={deleteParcelMutation.isPending}
                            onClick={() => deleteParcelMutation.mutate(selectedParcel.id)}
                          >
                            {deleteParcelMutation.isPending ? 'Deleting...' : 'Delete parcel'}
                          </button>
                        </div>
                        {deleteParcelMutation.isError && (
                          <p className="feedback-message feedback-error" role="alert">
                            Unable to delete parcel. Please try again.
                          </p>
                        )}
                      </>
                    ) : (
                      <>
                        <p className="parcel-crop">
                          Crop <strong>{selectedParcel.crop}</strong>
                        </p>
                        <Link
                          className="parcel-diagnosis-link"
                          to={`/diagnosis?parcelId=${encodeURIComponent(selectedParcel.id)}`}
                        >
                          Diagnose a leaf
                        </Link>
                      </>
                    )}
                  </div>
                )
              })()}
            </Popup>
          )}
        </MapContainer>

        <button
          className={`delete-mode-toggle${deleteMode ? ' delete-mode-active' : ''}`}
          type="button"
          title={deleteMode ? 'Exit parcel deletion mode' : 'Delete a parcel'}
          aria-label={deleteMode ? 'Exit parcel deletion mode' : 'Delete parcel mode'}
          aria-pressed={deleteMode}
          onClick={() => {
            setDeleteMode((active) => !active)
            setSelectedParcelId(null)
            setSelectedParcelPosition(null)
            deleteParcelMutation.reset()
          }}
        >
          {deleteMode ? (
            <svg aria-hidden="true" viewBox="0 0 24 24" focusable="false">
              <path d="M6 6l12 12M18 6 6 18" />
            </svg>
          ) : (
            <svg aria-hidden="true" viewBox="0 0 24 24" focusable="false">
              <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 3v6m4-6v6" />
            </svg>
          )}
        </button>

        {geometry && !cropsError && (
          <form
            onSubmit={(event) => {
              event.preventDefault()
              const formData = new FormData(event.currentTarget)

              createParcelMutation.mutate({
                name: String(formData.get('name') ?? '').trim(),
                crop: String(formData.get('crop') ?? ''),
                geometry,
              })
            }}
            className="parcel-form"
          >
            <div className="form-heading">
              <span className="eyebrow">NEW FIELD</span>
              <h2>Add a parcel</h2>
              <p>Give your field a name and choose what you grow.</p>
            </div>
            <label>
              Parcel name
              <input name="name" required maxLength={100} />
            </label>

            <label>
              Crop
              <select name="crop" required>
                <option value="">Select a crop</option>
                {crops.map((crop) => (
                  <option key={crop.crop} value={crop.crop}>
                    {crop.crop}
                  </option>
                ))}
              </select>
            </label>

            <div className="form-actions">
              <button type="submit" disabled={createParcelMutation.isPending}>
                {createParcelMutation.isPending ? 'Saving...' : 'Save parcel'}
              </button>
            </div>
          </form>
        )}
      </div>

      {drawError && (
        <p className="feedback-message feedback-error" role="alert">
          {drawError}
        </p>
      )}

      {parcelsError && (
        <p className="feedback-message feedback-error" role="alert">
          Unable to load parcels. Please try again.
        </p>
      )}

      {cropsError && (
        <p className="feedback-message feedback-error" role="alert">
          Unable to load crops. Please try again.
        </p>
      )}

      {createParcelMutation.isError && (
        <p className="feedback-message feedback-error" role="alert">
          Unable to save parcel. Please try again.
        </p>
      )}
    </section>
  )
}
