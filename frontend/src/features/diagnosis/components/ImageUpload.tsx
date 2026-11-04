import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { createDiagnosis, getParcels, type Diagnosis } from '../../../lib/apiClient'

interface ImageUploadProps {
  parcelId?: string
}

export function ImageUpload({ parcelId }: ImageUploadProps) {
  const { data: parcels = [], isError: parcelsError } = useQuery({
    queryKey: ['parcels'],
    queryFn: getParcels,
  })
  const [selectedParcelId, setSelectedParcelId] = useState(parcelId ?? '')
  const [file, setFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [diagnosis, setDiagnosis] = useState<Diagnosis | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl)
      }
    }
  }, [previewUrl])

  function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const selectedFile = event.target.files?.[0] ?? null

    setDiagnosis(null)
    setError(null)

    if (!selectedFile) {
      setFile(null)
      setPreviewUrl(null)
      return
    }

    if (!['image/jpeg', 'image/png'].includes(selectedFile.type)) {
      setFile(null)
      setPreviewUrl(null)
      setError('Please select a JPEG or PNG image.')
      return
    }

    if (selectedFile.size > 10 * 1024 * 1024) {
      setFile(null)
      setPreviewUrl(null)
      setError('The image must be smaller than 10 MB.')
      return
    }

    setFile(selectedFile)
    setPreviewUrl(URL.createObjectURL(selectedFile))
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!file) {
      setError('Please select an image first.')
      return
    }

    if (!selectedParcelId) {
      setError('Please select a parcel first.')
      return
    }

    setIsSubmitting(true)
    setError(null)

    try {
      const result = await createDiagnosis(file, selectedParcelId)
      setDiagnosis(result)
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Unable to submit the image.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section aria-labelledby="diagnosis-title">
      <h1 id="diagnosis-title">Leaf diagnosis</h1>

      <form onSubmit={handleSubmit}>
        <label htmlFor="diagnosis-parcel">Parcel</label>
        <select
          id="diagnosis-parcel"
          value={selectedParcelId}
          onChange={(event) => setSelectedParcelId(event.target.value)}
          disabled={parcels.length === 0}
          required
        >
          <option value="">Select a parcel</option>
          {parcels.map((parcel) => (
            <option key={parcel.id} value={parcel.id}>
              {parcel.name} ({parcel.crop})
            </option>
          ))}
        </select>

        <label htmlFor="leaf-image">Leaf image</label>
        <input
          id="leaf-image"
          type="file"
          accept="image/*"
          capture="environment"
          onChange={handleFileChange}
        />

        {previewUrl && (
          <img
            src={previewUrl}
            alt="Selected leaf preview"
            style={{ maxWidth: '100%', maxHeight: '300px' }}
          />
        )}

        <button type="submit" disabled={!file || isSubmitting}>
          {isSubmitting ? 'Submitting...' : 'Submit for diagnosis'}
        </button>
      </form>

      {parcelsError && <p role="alert">Unable to load parcels. Please try again.</p>}

      {error && <p role="alert">{error}</p>}

      {diagnosis && (
        <output aria-live="polite">
          Diagnosis: {diagnosis.topPredictions[0]?.disease ?? diagnosis.status}
        </output>
      )}
    </section>
  )
}
