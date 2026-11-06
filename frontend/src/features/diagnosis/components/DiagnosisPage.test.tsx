import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { createDiagnosis } from '../../../lib/apiClient'
import { DiagnosisPage } from './DiagnosisPage'

vi.mock('../../../lib/apiClient', async () => {
  const actual =
    await vi.importActual<typeof import('../../../lib/apiClient')>('../../../lib/apiClient')

  return {
    ...actual,
    createDiagnosis: vi.fn(),
  }
})

function renderDiagnosisPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <DiagnosisPage />
    </QueryClientProvider>,
  )
}

describe('DiagnosisPage', () => {
  it('submits a diagnosis for the selected parcel', async () => {
    const user = userEvent.setup()
    vi.mocked(createDiagnosis).mockResolvedValue({
      id: 'diagnosis-1',
      parcelId: '7c9e6679-7425-40de-944b-e07fc1f90ae7',
      status: 'confident',
      crop: 'tomato',
      cropCheck: 'ok',
      topPredictions: [
        {
          label: 'tomato_early_blight',
          disease: 'early_blight',
          probability: 0.92,
        },
      ],
      supportLevel: 'lab_only',
      modelVersion: 'plant-v1',
      createdAt: '2026-11-02T17:00:00Z',
    })
    renderDiagnosisPage()

    const parcelSelect = await screen.findByLabelText('Parcel')
    await user.selectOptions(parcelSelect, '7c9e6679-7425-40de-944b-e07fc1f90ae7')
    const file = new File(['leaf image'], 'leaf.png', { type: 'image/png' })
    await user.upload(screen.getByLabelText('Leaf image'), file)
    await user.click(screen.getByRole('button', { name: 'Submit for diagnosis' }))

    await waitFor(() => {
      expect(screen.getByText('Diagnosis: early_blight')).toBeInTheDocument()
    })
    expect(createDiagnosis).toHaveBeenCalledWith(
      file,
      '7c9e6679-7425-40de-944b-e07fc1f90ae7',
    )
  })
})
