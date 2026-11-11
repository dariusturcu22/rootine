import 'leaflet/dist/leaflet.css'
import 'leaflet-draw/dist/leaflet.draw.css'
import type { DrawEvents, LatLng, Polygon as LeafletPolygon, Layer } from 'leaflet'
import { useState, useRef, useEffect, useCallback, useMemo } from 'react'
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
  getErrorMessage,
  type Parcel,
  type ParcelGeometry,
  type Position,
} from '../../../lib/apiClient'
import {
  useCreateParcel,
  useCrops,
  useDeleteParcel,
  useParcelQueryClient,
  useParcels,
  useUpdateParcel,
} from '../api/hooks'
import './MapView.css'

type ParcelLayer = LeafletPolygon & { parcelId?: string }

const PARCEL_PATH_OPTIONS = {
  color: '#2E7D32',
  fillColor: '#C5E1A5',
  fillOpacity: 0.55,
}

function orientation(first: Position, second: Position, third: Position): number {
  return (
    (second[0] - first[0]) * (third[1] - first[1]) - (second[1] - first[1]) * (third[0] - first[0])
  )
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

function isValidPolygonRing(ring: Position[]): boolean {
  if (ring.length < 4) {
    return false
  }

  const first = ring[0]
  const last = ring.at(-1)
  if (
    !first ||
    !last ||
    first[0] !== last[0] ||
    first[1] !== last[1] ||
    ring.some(([longitude, latitude]) => !Number.isFinite(longitude) || !Number.isFinite(latitude))
  ) {
    return false
  }

  let area = 0
  for (let index = 0; index < ring.length - 1; index += 1) {
    const current = ring[index]
    const next = ring[index + 1]
    if (!current || !next) {
      return false
    }
    area += current[0] * next[1] - next[0] * current[1]
  }
  if (Math.abs(area) <= Number.EPSILON) {
    return false
  }

  const segmentCount = ring.length - 1
  for (let firstIndex = 0; firstIndex < segmentCount; firstIndex += 1) {
    const firstStart = ring[firstIndex]
    const firstEnd = ring[firstIndex + 1]
    if (!firstStart || !firstEnd) {
      return false
    }

    for (let secondIndex = firstIndex + 1; secondIndex < segmentCount; secondIndex += 1) {
      if (secondIndex === firstIndex + 1 || (firstIndex === 0 && secondIndex === segmentCount - 1)) {
        continue
      }
      const secondStart = ring[secondIndex]
      const secondEnd = ring[secondIndex + 1]
      if (secondStart && secondEnd && segmentsIntersect(firstStart, firstEnd, secondStart, secondEnd)) {
        return false
      }
    }
  }

  return true
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

function extractClosedRing(parcelLayer: LeafletPolygon): Position[] {
  const rawLatLngs = parcelLayer.getLatLngs()
  const firstLevel = Array.isArray(rawLatLngs[0]) ? rawLatLngs[0] : rawLatLngs
  const ringLatLngs = (Array.isArray(firstLevel[0]) ? firstLevel[0] : firstLevel) as LatLng[]
  const ring = ringLatLngs.map(({ lng, lat }): Position => [lng, lat])
  const first = ring[0]
  const last = ring.at(-1)

  if (first && last && (first[0] !== last[0] || first[1] !== last[1])) {
    ring.push([...first])
  }

  return ring
}

function ParcelPolygon({
  parcel,
  showLabel,
  onRegisterLayer,
  onSelectParcel,
}: {
  parcel: Parcel
  showLabel: boolean
  onRegisterLayer: (id: string, layer: LeafletPolygon | null) => void
  onSelectParcel: (id: string, position: [number, number]) => void
}) {
  const positions = useMemo(
    () =>
      parcel.geometry.coordinates[0].map(
        ([longitude, latitude]): [number, number] => [latitude, longitude],
      ),
    [parcel.geometry.coordinates],
  )

  return (
    <Polygon
      ref={(layer) => onRegisterLayer(parcel.id, layer)}
      eventHandlers={{
        click: (event) => {
          onSelectParcel(parcel.id, [event.latlng.lat, event.latlng.lng])
        },
      }}
      positions={positions}
      pathOptions={PARCEL_PATH_OPTIONS}
    >
      {showLabel && (
        <Tooltip permanent direction="center" className="parcel-name-label">
          {parcel.name}
        </Tooltip>
      )}
    </Polygon>
  )
}

function MapDrawEventsListener({
  onZoomChange,
  onEditStart,
  onVertexEdit,
  onDrawStart,
  onActionButtonClick,
}: {
  onZoomChange: (zoom: number) => void
  onEditStart: () => void
  onVertexEdit: (event: unknown) => void
  onDrawStart: () => void
  onActionButtonClick: (action: 'save' | 'cancel') => void
}) {
  const map = useMapEvents({
    zoomend: (event) => onZoomChange(event.target.getZoom()),
  })

  useEffect(() => {
    const container = map.getContainer()

    const handlePointerDown = (event: MouseEvent) => {
      const target = event.target as HTMLElement | null
      const actionLink = target?.closest('.leaflet-draw-actions a') as HTMLAnchorElement | null
      if (!actionLink) return

      const text = actionLink.textContent?.trim().toLowerCase()
      if (text === 'cancel') {
        onActionButtonClick('cancel')
      } else if (text === 'save') {
        onActionButtonClick('save')
      }
    }

    container.addEventListener('pointerdown', handlePointerDown, true)
    map.on('draw:editstart', onEditStart)
    map.on('draw:editvertex', onVertexEdit)
    map.on('draw:drawstart', onDrawStart)

    return () => {
      container.removeEventListener('pointerdown', handlePointerDown, true)
      map.off('draw:editstart', onEditStart)
      map.off('draw:editvertex', onVertexEdit)
      map.off('draw:drawstart', onDrawStart)
    }
  }, [map, onEditStart, onVertexEdit, onDrawStart, onActionButtonClick])

  return null
}

export function MapView() {
  const queryClient = useParcelQueryClient()
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
  const isCancelClickedRef = useRef(false)
  const pendingEditErrorRef = useRef<string | null>(null)
  const editedRingsRef = useRef(new Map<string, Position[]>())
  const hasInitializedParcelsRef = useRef(false)

  const { data: parcels = [], isError: parcelsError, error: parcelsQueryError } = useParcels()
  const { data: crops = [], isError: cropsError, error: cropsQueryError } = useCrops()

  const parcelsRef = useRef(parcels)
  const parcelLayersRef = useRef(new Map<string, ParcelLayer>())

  useEffect(() => {
    parcelsRef.current = parcels
    if (parcels.length > 0 && !hasInitializedParcelsRef.current) {
      hasInitializedParcelsRef.current = true
      setEditControlVersion((v) => v + 1)
    }
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

  const handleSelectParcel = useCallback((id: string, position: [number, number]) => {
    setSelectedParcelId(id)
    setSelectedParcelPosition(position)
  }, [])

  const computeCurrentEditError = useCallback((): string | null => {
    for (const [id, parcelLayer] of parcelLayersRef.current.entries()) {
      const parcel = parcelsRef.current.find((p) => p.id === id)
      if (!parcel) continue

      const ring = editedRingsRef.current.get(id) ?? extractClosedRing(parcelLayer)

      if (!isValidPolygonRing(ring)) {
        return 'The parcel geometry must be a valid polygon.'
      }

      const overlapsOther = parcelsRef.current.some(
        (otherParcel) =>
          otherParcel.id !== id && polygonsOverlap(ring, otherParcel.geometry.coordinates[0]),
      )

      if (overlapsOther) {
        return 'This parcel overlaps an existing parcel. Edit it in a different area.'
      }
    }

    return null
  }, [])

  const handleEditStart = useCallback(() => {
    isCancelClickedRef.current = false
    pendingEditErrorRef.current = null
    editedRingsRef.current.clear()
    setDrawError((prev) => (prev !== null ? null : prev))
  }, [])

  const handleDrawStart = useCallback(() => {
    isCancelClickedRef.current = false
    pendingEditErrorRef.current = null
    editedRingsRef.current.clear()
    setDrawError((prev) => (prev !== null ? null : prev))
  }, [])

  const handleVertexEdit = useCallback(
    (event: unknown) => {
      const poly = (event as { poly?: ParcelLayer })?.poly
      if (poly) {
        const ring = extractClosedRing(poly)
        const matchedId =
          poly.parcelId ??
          Array.from(parcelLayersRef.current.entries()).find(([, layer]) => layer === poly)?.[0]

        if (matchedId) {
          editedRingsRef.current.set(matchedId, ring)
        }
      }

      parcelLayersRef.current.forEach((layer, id) => {
        if (!editedRingsRef.current.has(id)) {
          editedRingsRef.current.set(id, extractClosedRing(layer))
        } else if (!poly) {
          editedRingsRef.current.set(id, extractClosedRing(layer))
        }
      })

      pendingEditErrorRef.current = computeCurrentEditError()
    },
    [computeCurrentEditError],
  )

  const handleActionButtonClick = useCallback(
    (action: 'save' | 'cancel') => {
      if (action === 'cancel') {
        isCancelClickedRef.current = true
        pendingEditErrorRef.current = null
        editedRingsRef.current.clear()
        return
      }

      isCancelClickedRef.current = false
      parcelLayersRef.current.forEach((layer, id) => {
        editedRingsRef.current.set(id, extractClosedRing(layer))
      })
      pendingEditErrorRef.current = computeCurrentEditError()
    },
    [computeCurrentEditError],
  )

  const createParcelMutation = useCreateParcel(async () => {
    temporaryLayer.current?.remove()
    temporaryLayer.current = null
    setGeometry(null)
    setDrawError(null)
    await queryClient.invalidateQueries({ queryKey: ['parcels'] })
    setEditControlVersion((v) => v + 1)
  })
  const updateParcelMutation = useUpdateParcel(
    async () => {
      await queryClient.invalidateQueries({ queryKey: ['parcels'] })
      setEditControlVersion((version) => version + 1)
    },
    async (error) => {
      setDrawError(getErrorMessage(error, 'Unable to update parcel. Please try again.'))
      await queryClient.invalidateQueries({ queryKey: ['parcels'] })
      setEditControlVersion((version) => version + 1)
    },
  )
  const deleteParcelMutation = useDeleteParcel(async (deletedParcelId) => {
    setSelectedParcelId(null)
    setSelectedParcelPosition(null)
    setDeleteMode(false)
    queryClient.setQueryData<Parcel[]>(['parcels'], (currentParcels = []) =>
      currentParcels.filter(({ id }) => id !== deletedParcelId),
    )
    await queryClient.invalidateQueries({ queryKey: ['parcels'] })
  })

  const updateParcelMutationRef = useRef(updateParcelMutation)
  useEffect(() => {
    updateParcelMutationRef.current = updateParcelMutation
  }, [updateParcelMutation])

  const handlePolygonCreated = (event: DrawEvents.Created) => {
    temporaryLayer.current = event.layer
    setDrawError(null)

    const layer = event.layer as LeafletPolygon
    const coordinates = extractClosedRing(layer)

    if (coordinates.length >= 4) {
      if (!isValidPolygonRing(coordinates)) {
        temporaryLayer.current?.remove()
        temporaryLayer.current = null
        setGeometry(null)
        setDrawError('The parcel geometry must be a valid polygon.')
        return
      }

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

  const handleParcelsEdited = useCallback(
    (event: DrawEvents.Edited) => {
      if (pendingEditErrorRef.current) {
        parcelLayersRef.current.forEach((parcelLayer, id) => {
          const parcel = parcelsRef.current.find((p) => p.id === id)
          if (parcel) {
            parcelLayer.setLatLngs(
              parcel.geometry.coordinates[0].map(([longitude, latitude]) => [latitude, longitude]),
            )
          }
        })
        setDrawError(pendingEditErrorRef.current)
        editedRingsRef.current.clear()
        return
      }

      event.layers.eachLayer((layer) => {
        const parcelLayer = layer as ParcelLayer
        const parcel = parcelsRef.current.find(
          ({ id }) => id === parcelLayer.parcelId || parcelLayersRef.current.get(id) === parcelLayer,
        )
        if (!parcel) return

        const latLngs = editedRingsRef.current.get(parcel.id) ?? extractClosedRing(parcelLayer)
        editedRingsRef.current.delete(parcel.id)

        if (!isValidPolygonRing(latLngs)) {
          parcelLayer.setLatLngs(
            parcel.geometry.coordinates[0].map(([longitude, latitude]) => [latitude, longitude]),
          )
          pendingEditErrorRef.current = 'The parcel geometry must be a valid polygon.'
          setDrawError(pendingEditErrorRef.current)
          return
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
          pendingEditErrorRef.current =
            'This parcel overlaps an existing parcel. Edit it in a different area.'
          setDrawError(pendingEditErrorRef.current)
          return
        }

        setDrawError(null)
        const updatedGeometry: ParcelGeometry = { type: 'Polygon', coordinates: [latLngs] }
        queryClient.setQueryData<Parcel[]>(['parcels'], (currentParcels = []) =>
          currentParcels.map((currentParcel) =>
            currentParcel.id === parcel.id
              ? { ...currentParcel, geometry: updatedGeometry }
              : currentParcel,
          ),
        )
        parcelLayer.setStyle(PARCEL_PATH_OPTIONS)
        updateParcelMutationRef.current.mutate({
          id: parcel.id,
          parcel: {
            name: parcel.name,
            crop: parcel.crop,
            geometry: updatedGeometry,
          },
        })
      })
    },
    [queryClient],
  )

  const handleParcelEditStop = useCallback(() => {
    if (!isCancelClickedRef.current) {
      const errorToDisplay = pendingEditErrorRef.current ?? computeCurrentEditError()
      if (errorToDisplay) {
        setDrawError(errorToDisplay)
      }
    }

    isCancelClickedRef.current = false
    pendingEditErrorRef.current = null
    editedRingsRef.current.clear()

    window.setTimeout(() => {
      setEditControlVersion((version) => version + 1)
    }, 0)
  }, [computeCurrentEditError])

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
          <MapDrawEventsListener
            onZoomChange={setMapZoom}
            onEditStart={handleEditStart}
            onVertexEdit={handleVertexEdit}
            onDrawStart={handleDrawStart}
            onActionButtonClick={handleActionButtonClick}
          />
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
              <ParcelPolygon
                key={parcel.id}
                parcel={parcel}
                showLabel={mapZoom >= 9}
                onRegisterLayer={registerParcelLayer}
                onSelectParcel={handleSelectParcel}
              />
            ))}

            <EditControl
              position="topright"
              onCreated={handlePolygonCreated}
              onEdited={handleParcelsEdited}
              onEditStop={handleParcelEditStop}
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
                            {getErrorMessage(
                              deleteParcelMutation.error,
                              'Unable to delete parcel. Please try again.',
                            )}
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

        {drawError && (
          <p className="map-feedback-message feedback-message feedback-error" role="alert">
            {drawError}
          </p>
        )}
      </div>

      {parcelsError && (
        <p className="feedback-message feedback-error" role="alert">
          {getErrorMessage(parcelsQueryError, 'Unable to load parcels. Please try again.')}
        </p>
      )}

      {cropsError && (
        <p className="feedback-message feedback-error" role="alert">
          {getErrorMessage(cropsQueryError, 'Unable to load crops. Please try again.')}
        </p>
      )}

      {createParcelMutation.isError && (
        <p className="feedback-message feedback-error" role="alert">
          {getErrorMessage(createParcelMutation.error, 'Unable to save parcel. Please try again.')}
        </p>
      )}
    </section>
  )
}