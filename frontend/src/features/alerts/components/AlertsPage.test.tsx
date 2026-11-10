import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { AlertsPage } from './AlertsPage'

describe('AlertsPage', () => {
  it('shows the live-alert setup state without implying alerts are connected', () => {
    render(
      <MemoryRouter>
        <AlertsPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Alerts' })).toBeInTheDocument()
    expect(screen.getByText('Stream not connected')).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: 'Live alerts are not connected yet' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View field risk' })).toHaveAttribute('href', '/risk')
    expect(screen.getByText('No alerts in demo1.')).toBeInTheDocument()
  })
})
