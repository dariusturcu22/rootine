import { useQueries, useQuery } from '@tanstack/react-query'
import type { UseQueryResult } from '@tanstack/react-query'
import { useState } from 'react'
import { MapContainer, Polygon, Popup, TileLayer, Tooltip, useMapEvents } from 'react-leaflet'
import { MAP_CENTER, MAP_ZOOM } from '../../../config/env'
import { ApiError, getParcelRisk, getParcels } from '../../../lib/apiClient'
import type { Parcel, ParcelRisk, RiskLevel } from '../../../lib/apiClient'
import './RiskView.css'

const riskLabels: Record<RiskLevel, string> = {
  low: 'Low risk',
  medium: 'Moderate risk',
  high: 'High risk',
  not_modelled: 'Not modelled',
}

const riskColors: Record<RiskLevel, { fill: string; stroke: string }> = {
  low: { fill: '#8fc99b', stroke: '#388e4c' },
  medium: { fill: '#f0d76e', stroke: '#b18a12' },
  high: { fill: '#c76a5f', stroke: '#9e3f36' },
  not_modelled: { fill: '#c8cec8', stroke: '#7c887d' },
}

function RiskZoomListener({ onZoomChange }: { onZoomChange: (zoom: number) => void }) {
  useMapEvents({
    zoomend: (event) => onZoomChange(event.target.getZoom()),
  })

  return null
}

function formatGeneratedAt(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? 'Unknown'
    : date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

function getRiskErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 404) {
    return 'A risk result has not been generated for this field yet.'
  }
  if (error instanceof ApiError && error.status === 501) {
    return 'Risk results are not available yet.'
  }
  return 'Unable to load risk for this field.'
}

export function RiskView() {
  const [mapZoom, setMapZoom] = useState(MAP_ZOOM)
  const [selectedParcel, setSelectedParcel] = useState<{
    parcel: Parcel
    position: [number, number]
  } | null>(null)

  const parcelsQuery = useQuery({
    queryKey: ['parcels'],
    queryFn: getParcels,
  })
  const parcels = parcelsQuery.data ?? []

  const riskQueries = useQueries({
    queries: parcels.map((parcel) => ({
      queryKey: ['parcels', parcel.id, 'risk'],
      queryFn: () => getParcelRisk(parcel.id),
      retry: false,
    })),
  })

  if (parcelsQuery.isPending) {
    return (
      <section className="risk-page">
        <RiskPageHeading />
        <p className="risk-message" role="status">
          Loading your parcels...
        </p>
      </section>
    )
  }

  if (parcelsQuery.isError) {
    return (
      <section className="risk-page">
        <RiskPageHeading />
        <p className="risk-message risk-message-error" role="alert">
          Unable to load your parcels right now. Please try again later.
        </p>
      </section>
    )
  }

  const risks = riskQueries.flatMap((query) => (query.data ? [query.data] : []))
  const countRisk = (level: RiskLevel) => risks.filter((risk) => risk.level === level).length
  const newestRisk = risks.reduce<string | null>(
    (latest, risk) => (latest === null || risk.generatedAt > latest ? risk.generatedAt : latest),
    null,
  )

  return (
    <section className="risk-page">
      <header className="page-heading risk-heading">
        <div>
          <p className="eyebrow">WEATHER-BASED INSIGHTS</p>
          <h1>Field risk</h1>
          <p className="page-description">
            See the latest weather-based disease risk for each field.
          </p>
        </div>
        <span className="risk-updated">
          {newestRisk ? `Latest assessment ${formatGeneratedAt(newestRisk)}` : 'Risk assessment'}
        </span>
      </header>

      <div className="risk-summary" aria-label="Risk summary">
        {(['high', 'medium', 'low', 'not_modelled'] as const).map((level) => (
          <div className={`risk-summary-item risk-summary-${level}`} key={level}>
            <span className="risk-summary-dot" aria-hidden="true" />
            <strong>{countRisk(level)}</strong>
            <span>{riskLabels[level]}</span>
          </div>
        ))}
        <div className="risk-summary-item risk-summary-no-data">
          <span className="risk-summary-dot" aria-hidden="true" />
          <strong>{riskQueries.filter((query) => query.isPending || query.isError).length}</strong>
          <span>Pending or unavailable</span>
        </div>
      </div>

      <div className="risk-map-frame" aria-label="Parcel weather risk map">
        <MapContainer center={MAP_CENTER} zoom={MAP_ZOOM} className="risk-leaflet-map">
          <RiskZoomListener onZoomChange={setMapZoom} />
          <TileLayer
            attribution="Tiles &copy; Esri"
            url="https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
            maxZoom={19}
          />
          <TileLayer
            url="https://services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"
            maxZoom={19}
          />
          {parcels.map((parcel, index) => {
            const riskQuery = riskQueries[index]
            const risk = riskQuery?.data
            const colors = risk ? riskColors[risk.level] : { fill: '#aeb8ae', stroke: '#748074' }

            return (
              <Polygon
                key={parcel.id}
                positions={parcel.geometry.coordinates[0].map(([longitude, latitude]) => [
                  latitude,
                  longitude,
                ])}
                pathOptions={{
                  color: colors.stroke,
                  fillColor: colors.fill,
                  fillOpacity: 0.72,
                  weight: selectedParcel?.parcel.id === parcel.id ? 4 : 2,
                }}
                eventHandlers={{
                  click: (event) =>
                    setSelectedParcel({
                      parcel,
                      position: [event.latlng.lat, event.latlng.lng],
                    }),
                }}
              >
                {mapZoom >= 9 && (
                  <Tooltip permanent direction="center" className="parcel-name-label">
                    {parcel.name}
                  </Tooltip>
                )}
              </Polygon>
            )
          })}
          {selectedParcel && (
            <Popup
              position={selectedParcel.position}
              className="risk-popup"
              eventHandlers={{
                remove: () => setSelectedParcel(null),
              }}
            >
              <RiskParcelCard
                parcel={selectedParcel.parcel}
                query={riskQueries[parcels.findIndex(({ id }) => id === selectedParcel.parcel.id)]}
              />
            </Popup>
          )}
        </MapContainer>
        <RiskLegend />
      </div>
    </section>
  )
}

function RiskPageHeading() {
  return (
    <header className="page-heading">
      <div>
        <p className="eyebrow">WEATHER-BASED INSIGHTS</p>
        <h1>Field risk</h1>
      </div>
    </header>
  )
}

function RiskLegend() {
  return (
    <div className="risk-legend" aria-label="Risk level legend">
      {(['high', 'medium', 'low', 'not_modelled'] as const).map((level) => (
        <span className="risk-legend-item" key={level}>
          <span className={`risk-legend-swatch risk-legend-${level}`} aria-hidden="true" />
          {riskLabels[level]}
        </span>
      ))}
      <span className="risk-legend-item">
        <span className="risk-legend-swatch risk-legend-no-data" aria-hidden="true" />
        Pending or unavailable
      </span>
    </div>
  )
}

function RiskParcelCard({
  parcel,
  query,
}: {
  parcel: Parcel
  query: UseQueryResult<ParcelRisk, Error> | undefined
}) {
  const risk: ParcelRisk | undefined = query?.data

  return (
    <div className="risk-parcel-card">
      <span className="eyebrow">FIELD RISK</span>
      <h2>{parcel.name}</h2>
      <p className="risk-parcel-crop">
        Crop <strong>{parcel.crop}</strong>
      </p>
      {!risk ? (
        <p
          className={`risk-no-result${query?.isError ? ' risk-no-result-error' : ''}`}
          role={query?.isError ? 'alert' : 'status'}
        >
          {query?.isError
            ? getRiskErrorMessage(query.error)
            : 'Loading the latest risk assessment...'}
        </p>
      ) : (
        <>
          <div className={`risk-level risk-level-${risk.level}`}>
            <span className="risk-level-dot" aria-hidden="true" />
            {riskLabels[risk.level]}
          </div>
          {risk.level !== 'not_modelled' && (
            <div className="risk-index-block">
              <div className="risk-index-label">
                <span>Risk index</span>
                <strong>{risk.riskIndex}/100</strong>
              </div>
              <div
                className="risk-index-track"
                role="progressbar"
                aria-label="Risk index"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={risk.riskIndex}
              >
                <span
                  className={`risk-index-fill risk-index-fill-${risk.level}`}
                  style={{ width: `${Math.min(100, Math.max(0, risk.riskIndex))}%` }}
                />
              </div>
            </div>
          )}
          <p className="risk-reason">{risk.reason}</p>
          {risk.diseases.length > 0 && (
            <div className="risk-diseases">
              <h3>Disease risks</h3>
              {risk.diseases.map((disease) => (
                <div className="risk-disease-row" key={`${disease.disease}-${disease.model}`}>
                  <div>
                    <strong>{disease.disease.replaceAll('_', ' ')}</strong>
                    <span>{disease.model} model</span>
                  </div>
                  <span className={`disease-risk-badge disease-risk-${disease.level}`}>
                    {riskLabels[disease.level]}
                  </span>
                </div>
              ))}
            </div>
          )}
          <p className="risk-generated">
            Assessment generated <strong>{formatGeneratedAt(risk.generatedAt)}</strong>
          </p>
        </>
      )}
    </div>
  )
}
