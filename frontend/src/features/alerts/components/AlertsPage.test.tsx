import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AlertsPage } from './AlertsPage'

const streamState = vi.hoisted(() => ({
  status: 'connecting',
  alerts: [] as Array<{
    parcelId?: string
    parcelName?: string
    crop?: string
    level?: 'high'
    riskIndex?: number
    reason?: string
    generatedAt?: string
  }>,
  errorMessage: null as string | null,
}))

vi.mock('../hooks/useAlertStream', () => ({
  useAlertStream: () => streamState,
}))

describe('AlertsPage', () => {
  beforeEach(() => {
    streamState.status = 'connecting'
    streamState.alerts = []
    streamState.errorMessage = null
  })

  it('shows the connecting state', () => {
    render(
      <MemoryRouter>
        <AlertsPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Alerts' })).toBeInTheDocument()
    expect(screen.getByText('Connecting')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Connecting to live alerts' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View field risk' })).toHaveAttribute('href', '/risk')
    expect(screen.getByText('Alerts stay here for this session only.')).toBeInTheDocument()
  })

  it('shows incoming risk alerts in a live feed', () => {
    streamState.status = 'connected'
    streamState.alerts = [
      {
        parcelId: '3b241101-e2bb-4255-8caf-4136c566a962',
        parcelName: 'North field',
        crop: 'potato',
        level: 'high',
        riskIndex: 78,
        reason: 'High risk conditions were detected.',
        generatedAt: '2026-11-01T06:00:00Z',
      },
    ]

    render(
      <MemoryRouter>
        <AlertsPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('Live')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Live risk alerts' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'North field' })).toBeInTheDocument()
    expect(screen.getByText(/potato/)).toBeInTheDocument()
    expect(screen.getByText(/78\/100/)).toBeInTheDocument()
    expect(screen.getByText('High risk conditions were detected.')).toBeInTheDocument()
  })

  it('shows a friendly stream error', () => {
    streamState.status = 'error'
    streamState.errorMessage = 'The live alert service is unavailable.'

    render(
      <MemoryRouter>
        <AlertsPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('Stream unavailable')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Alerts could not be loaded' })).toBeInTheDocument()
    expect(screen.getByText('The live alert service is unavailable.')).toBeInTheDocument()
  })
})
