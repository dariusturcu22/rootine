import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../store/authStore'
import { login } from '../../../lib/apiClient'
import { LoginPage } from './LoginPage'

vi.mock('../../../lib/apiClient', async () => {
  const actual =
    await vi.importActual<typeof import('../../../lib/apiClient')>('../../../lib/apiClient')

  return {
    ...actual,
    login: vi.fn(),
  }
})

describe('LoginPage', () => {
  afterEach(() => {
    useAuthStore.getState().clear()
    vi.clearAllMocks()
  })

  it('stores the returned token after successful login', async () => {
    vi.mocked(login).mockResolvedValue({
      accessToken: 'new-access-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
    })

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('Email'), {
      target: { value: 'farmer@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'correct-password' } })
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => {
      expect(useAuthStore.getState().token).toBe('new-access-token')
    })
    expect(login).toHaveBeenCalledWith({
      email: 'farmer@example.com',
      password: 'correct-password',
    })
  })

  it('shows the session-expired message only after an expired session redirect', () => {
    const { unmount } = render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('Sign in to continue.')).toBeInTheDocument()
    expect(screen.queryByText('Your session expired. Sign in to continue.')).not.toBeInTheDocument()

    unmount()
    render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { sessionExpired: true } }]}>
        <LoginPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('Your session expired. Sign in to continue.')).toBeInTheDocument()
  })
})
