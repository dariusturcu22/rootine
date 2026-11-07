import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createDiagnosis } from '../../../lib/apiClient'
import { ImageUpload } from './ImageUpload'

vi.mock('../../../lib/apiClient', async () => {
  const actual =
    await vi.importActual<typeof import('../../../lib/apiClient')>('../../../lib/apiClient')

  return {
    ...actual,
    createDiagnosis: vi.fn(),
  }
})

describe('ImageUpload', () => {
  beforeEach(() => {
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:leaf-preview')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('disables submission until an image is selected', () => {
    render(<ImageUpload parcelId="parcel-1" parcelName="North field" onResultChange={vi.fn()} />)

    expect(screen.getByRole('button', { name: 'Submit for diagnosis' })).toBeDisabled()
  })

  it('rejects unsupported image formats', async () => {
    const user = userEvent.setup({ applyAccept: false })
    const file = new File(['not an image'], 'leaf.gif', { type: 'image/gif' })

    render(<ImageUpload parcelId="parcel-1" parcelName="North field" onResultChange={vi.fn()} />)

    await user.upload(screen.getByLabelText(/Browse files or take a photo/), file)

    expect(screen.getByRole('alert')).toHaveTextContent('Please select a JPEG or PNG image.')
    expect(screen.getByRole('button', { name: 'Submit for diagnosis' })).toBeDisabled()
  })

  it('shows a preview and diagnosis after submitting a valid image', async () => {
    const user = userEvent.setup()
    const file = new File(['leaf image'], 'leaf.png', { type: 'image/png' })
    vi.mocked(createDiagnosis).mockResolvedValue({
      id: 'diagnosis-1',
      parcelId: 'parcel-1',
      status: 'confident',
      crop: 'potato',
      cropCheck: 'ok',
      topPredictions: [
        {
          label: 'potato_early_blight',
          disease: 'early_blight',
          probability: 0.92,
        },
      ],
      supportLevel: 'lab_only',
      modelVersion: 'plant-v1',
      createdAt: '2026-11-02T17:00:00Z',
    })

    render(<ImageUpload parcelId="parcel-1" parcelName="North field" onResultChange={vi.fn()} />)

    await user.upload(screen.getByLabelText(/Browse files or take a photo/), file)

    expect(screen.getByAltText('Selected leaf preview')).toHaveAttribute('src', 'blob:leaf-preview')

    await user.click(screen.getByRole('button', { name: 'Submit for diagnosis' }))

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Most likely diseases' })).toBeInTheDocument()
    })
    expect(screen.getByText('early blight')).toBeInTheDocument()
    expect(screen.getByText(/Analyzed leaf from/)).toHaveTextContent('North field')
    expect(screen.getByRole('progressbar', { name: 'early blight confidence' })).toHaveAttribute(
      'aria-valuenow',
      '92',
    )
    expect(createDiagnosis).toHaveBeenCalledWith(file, 'parcel-1')
  })

  it('shows each returned prediction with its confidence bar', async () => {
    const user = userEvent.setup()
    vi.mocked(createDiagnosis).mockResolvedValue({
      id: 'diagnosis-2',
      parcelId: 'parcel-1',
      status: 'confident',
      crop: 'potato',
      cropCheck: 'ok',
      topPredictions: [
        { label: 'potato_early_blight', disease: 'early_blight', probability: 0.78 },
        { label: 'potato_late_blight', disease: 'late_blight', probability: 0.12 },
        { label: 'potato_healthy', disease: 'healthy', probability: 0.1 },
      ],
      supportLevel: 'lab_only',
      modelVersion: 'plant-v1',
      createdAt: '2026-11-02T17:00:00Z',
    })
    render(<ImageUpload parcelId="parcel-1" parcelName="North field" onResultChange={vi.fn()} />)

    await user.upload(
      screen.getByLabelText(/Browse files or take a photo/),
      new File(['leaf image'], 'leaf.png', { type: 'image/png' }),
    )
    await user.click(screen.getByRole('button', { name: 'Submit for diagnosis' }))

    expect(await screen.findAllByRole('progressbar')).toHaveLength(3)
    expect(screen.getByRole('progressbar', { name: 'early blight confidence' })).toHaveAttribute(
      'aria-valuenow',
      '78',
    )
    expect(screen.getByRole('progressbar', { name: 'late blight confidence' })).toHaveAttribute(
      'aria-valuenow',
      '12',
    )
    expect(screen.getByRole('progressbar', { name: 'healthy confidence' })).toHaveAttribute(
      'aria-valuenow',
      '10',
    )
  })

  it('returns to a fresh upload when starting a new review', async () => {
    const user = userEvent.setup()
    vi.mocked(createDiagnosis).mockResolvedValue({
      id: 'diagnosis-3',
      parcelId: 'parcel-1',
      status: 'confident',
      crop: 'potato',
      cropCheck: 'ok',
      topPredictions: [],
      supportLevel: 'lab_only',
      modelVersion: 'plant-v1',
      createdAt: '2026-11-02T17:00:00Z',
    })
    const onResultChange = vi.fn()
    render(
      <ImageUpload parcelId="parcel-1" parcelName="North field" onResultChange={onResultChange} />,
    )

    await user.upload(
      screen.getByLabelText(/Browse files or take a photo/),
      new File(['leaf image'], 'leaf.png', { type: 'image/png' }),
    )
    await user.click(screen.getByRole('button', { name: 'Submit for diagnosis' }))
    await user.click(await screen.findByRole('button', { name: 'New review' }))

    expect(screen.getByRole('heading', { name: 'Add a leaf photo' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Most likely diseases' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Submit for diagnosis' })).toBeDisabled()
    expect(onResultChange).toHaveBeenLastCalledWith(false)
  })
})
