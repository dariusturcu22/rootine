import { useEffect, useRef, useState } from 'react'
import { flushSync } from 'react-dom'
import { createDiagnosis, type Diagnosis } from '../api/diagnosisApi'
import './ImageUpload.css'

type ViewTransitionDocument = Document & {
  startViewTransition?: (update: () => void) => { finished: Promise<void> }
}

interface ImageUploadProps {
  parcelId: string
  parcelName: string
  onResultChange: (hasResult: boolean) => void
}

export function ImageUpload({ parcelId, parcelName, onResultChange }: ImageUploadProps) {
  const [file, setFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [diagnosis, setDiagnosis] = useState<Diagnosis | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  function showDiagnosis(result: Diagnosis) {
    const update = () =>
      flushSync(() => {
        setDiagnosis(result)
        onResultChange(true)
      })
    const transitionDocument = document as ViewTransitionDocument

    if (
      transitionDocument.startViewTransition &&
      !window.matchMedia('(prefers-reduced-motion: reduce)').matches
    ) {
      transitionDocument.startViewTransition(update)
      return
    }

    setDiagnosis(result)
    onResultChange(true)
  }

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
    onResultChange(false)
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

  function handleNewReview() {
    setDiagnosis(null)
    setFile(null)
    setPreviewUrl(null)
    setError(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
    onResultChange(false)
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!file) {
      setError('Please select an image first.')
      return
    }

    setIsSubmitting(true)
    setError(null)

    try {
      const result = await createDiagnosis(file, parcelId)
      showDiagnosis(result)
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Unable to submit the image.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className={`upload-card${diagnosis ? ' has-result' : ''}`}>
      {!diagnosis && (
        <div className="upload-controls">
          <div className="upload-heading">
            <span className="diagnosis-step">02</span>
            <div>
              <h2 id="upload-title">Add a leaf photo</h2>
              <p className="upload-intro">Use a well-lit JPEG or PNG image, up to 10 MB.</p>
            </div>
          </div>

          <form className="upload-form" onSubmit={handleSubmit}>
            <label className="upload-dropzone" htmlFor="leaf-image">
              <span className="upload-icon" aria-hidden="true">
                ↑
              </span>
              <span className="upload-prompt">
                <strong>{file ? file.name : 'Choose a photo'}</strong>
                <span>{file ? 'Select another image' : 'Browse files or take a photo'}</span>
              </span>
              <input
                ref={fileInputRef}
                id="leaf-image"
                type="file"
                accept="image/jpeg,image/png"
                capture="environment"
                onChange={handleFileChange}
              />
            </label>

            {previewUrl && (
              <div className="preview-frame">
                <img src={previewUrl} alt="Selected leaf preview" />
              </div>
            )}

            <button className="diagnosis-submit" type="submit" disabled={!file || isSubmitting}>
              {isSubmitting ? 'Checking your photo...' : 'Submit for diagnosis'}
            </button>
          </form>

          {error && (
            <p className="feedback-message feedback-error" role="alert">
              {error}
            </p>
          )}
        </div>
      )}

      {diagnosis && (
        <section
          className="diagnosis-result"
          aria-labelledby="diagnosis-result-title"
          aria-live="polite"
        >
          <div className="diagnosis-result-top">
            {previewUrl && (
              <div className="diagnosis-result-image">
                <img src={previewUrl} alt="Leaf submitted for diagnosis" />
              </div>
            )}
            <div className="diagnosis-result-summary">
              <span className="result-label">DIAGNOSIS RESULT</span>
              <p className="result-parcel">
                Analyzed leaf from <strong>{parcelName}</strong>
              </p>
              <span className="result-crop">{diagnosis.crop}</span>
              <span className={`result-status result-status-${diagnosis.status}`}>
                {diagnosis.status.replaceAll('_', ' ')}
              </span>
              {diagnosis.cropCheck === 'mismatch' && (
                <p className="result-note">The photo may not match the selected crop.</p>
              )}
            </div>
          </div>

          <div className="prediction-list">
            <h3 id="diagnosis-result-title">Most likely diseases</h3>
            {diagnosis.topPredictions.length > 0 ? (
              diagnosis.topPredictions.map((prediction, index) => {
                const confidence = Math.round(prediction.probability * 100)
                const barWidth = Math.min(100, Math.max(0, prediction.probability * 100))

                return (
                  <div
                    className={`prediction-row${index === 0 ? ' prediction-row-top' : ''}`}
                    key={prediction.label}
                  >
                    <span className="prediction-name">
                      {prediction.disease.replaceAll('_', ' ')}
                    </span>
                    <span
                      className="prediction-bar"
                      role="progressbar"
                      aria-label={`${prediction.disease.replaceAll('_', ' ')} confidence`}
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-valuenow={confidence}
                    >
                      <span style={{ width: `${barWidth}%` }} />
                    </span>
                    <span className="prediction-percent">{confidence}%</span>
                  </div>
                )
              })
            ) : (
              <p className="prediction-empty">
                No disease predictions are available for this photo.
              </p>
            )}
          </div>
          <button className="new-review-button" type="button" onClick={handleNewReview}>
            New review
          </button>
        </section>
      )}
    </section>
  )
}
