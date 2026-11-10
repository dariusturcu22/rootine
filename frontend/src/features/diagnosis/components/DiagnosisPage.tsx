import { useState } from 'react'
import { useParcels } from '../../maps/api/hooks'
import { ImageUpload } from './ImageUpload'
import './DiagnosisPage.css'

export function DiagnosisPage() {
  const [selectedParcelId, setSelectedParcelId] = useState(
    () => new URLSearchParams(window.location.search).get('parcelId') ?? '',
  )
  const [hasResult, setHasResult] = useState(false)
  const { data: parcels = [], isPending, isError } = useParcels()

  if (isPending) {
    return (
      <section className="diagnosis-page">
        <p className="eyebrow">CROP HEALTH</p>
        <h1>Leaf diagnosis</h1>
        <p className="diagnosis-status" role="status">
          Loading your parcels...
        </p>
      </section>
    )
  }

  if (isError) {
    return (
      <section className="diagnosis-page">
        <p className="eyebrow">CROP HEALTH</p>
        <h1>Leaf diagnosis</h1>
        <p className="feedback-message feedback-error" role="alert">
          Unable to load parcels. Please try again.
        </p>
      </section>
    )
  }

  if (parcels.length === 0) {
    return (
      <section className="diagnosis-page">
        <header className="page-heading">
          <div>
            <p className="eyebrow">CROP HEALTH</p>
            <h1>Leaf diagnosis</h1>
            <p className="page-description">
              Upload a clear leaf photo to check for common crop diseases.
            </p>
          </div>
        </header>
        <div className="empty-state">
          <span className="empty-state-mark" aria-hidden="true">
            +
          </span>
          <h2>Start with a field parcel</h2>
          <p>Create a parcel on the map before submitting a diagnosis.</p>
        </div>
      </section>
    )
  }

  const activeParcelId = parcels.some(({ id }) => id === selectedParcelId)
    ? selectedParcelId
    : (parcels[0]?.id ?? '')

  const activeParcel = parcels.find(({ id }) => id === activeParcelId)

  return (
    <section className="diagnosis-page">
      <header className="page-heading">
        <div>
          <p className="eyebrow">CROP HEALTH</p>
          <h1>Leaf diagnosis</h1>
          <p className="page-description">
            Upload a clear leaf photo to check for common crop diseases.
          </p>
        </div>
      </header>

      {!hasResult && (
        <div className="diagnosis-card">
          <div className="diagnosis-card-heading">
            <span className="diagnosis-step">01</span>
            <div>
              <h2>Choose a field</h2>
              <p>Select the parcel where the leaf was collected.</p>
            </div>
          </div>
          <label className="diagnosis-field" htmlFor="diagnosis-parcel">
            Parcel
            <select
              id="diagnosis-parcel"
              value={activeParcelId}
              onChange={(event) => setSelectedParcelId(event.target.value)}
            >
              {parcels.map((parcel) => (
                <option key={parcel.id} value={parcel.id}>
                  {parcel.name} ({parcel.crop})
                </option>
              ))}
            </select>
          </label>
          {activeParcel && (
            <p className="selected-parcel">
              Selected crop <strong>{activeParcel.crop}</strong>
            </p>
          )}
        </div>
      )}
      {activeParcel && (
        <ImageUpload
          parcelId={activeParcel.id}
          parcelName={activeParcel.name}
          onResultChange={setHasResult}
        />
      )}
    </section>
  )
}
