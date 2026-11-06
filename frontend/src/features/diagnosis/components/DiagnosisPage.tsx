import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { getParcels } from '../../../lib/apiClient'
import { ImageUpload } from './ImageUpload'

export function DiagnosisPage() {
  const [selectedParcelId, setSelectedParcelId] = useState('')
  const { data: parcels = [], isPending, isError } = useQuery({
    queryKey: ['parcels'],
    queryFn: getParcels,
  })

  if (isPending) {
    return <p>Loading parcels...</p>
  }

  if (isError) {
    return <p role="alert">Unable to load parcels. Please try again.</p>
  }

  if (parcels.length === 0) {
    return <p>Create a parcel on the map before submitting a diagnosis.</p>
  }

  const activeParcelId = parcels.some(({ id }) => id === selectedParcelId)
    ? selectedParcelId
    : (parcels[0]?.id ?? '')

  return (
    <>
      <label htmlFor="diagnosis-parcel">Parcel</label>
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
      {activeParcelId && <ImageUpload parcelId={activeParcelId} />}
    </>
  )
}
