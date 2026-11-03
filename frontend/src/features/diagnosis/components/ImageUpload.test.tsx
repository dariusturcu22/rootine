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
    render(<ImageUpload parcelId="parcel-1" />)

    expect(screen.getByRole('button', { name: 'Submit for diagnosis' })).toBeDisabled()
  })

  it('rejects unsupported image formats', async () => {
    const user = userEvent.setup()
    const file = new File(['not an image'], 'leaf.gif', { type: 'image/gif' })

    render(<ImageUpload parcelId="parcel-1" />)

    await user.upload(screen.getByLabelText('Leaf image'), file)

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

    render(<ImageUpload parcelId="parcel-1" />)

    await user.upload(screen.getByLabelText('Leaf image'), file)

    expect(screen.getByAltText('Selected leaf preview')).toHaveAttribute('src', 'blob:leaf-preview')

    await user.click(screen.getByRole('button', { name: 'Submit for diagnosis' }))

    await waitFor(() => {
      expect(screen.getByText('Diagnosis: early_blight')).toBeInTheDocument()
    })
    expect(createDiagnosis).toHaveBeenCalledWith(file, 'parcel-1')
  })
})
