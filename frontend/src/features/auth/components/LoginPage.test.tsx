import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../store/authStore'
import { ApiError, login, register } from '../../../lib/apiClient'
import { LoginPage } from './LoginPage'

vi.mock('../../../lib/apiClient', async () => {
  const actual =
    await vi.importActual<typeof import('../../../lib/apiClient')>('../../../lib/apiClient')

  return {
    ...actual,
    login: vi.fn(),
    register: vi.fn(),
  }
})

vi.mock('./LoginMapBackdrop', () => ({
  LoginMapBackdrop: () => null,
}))

afterEach(() => {
  useAuthStore.getState().clear()
  vi.clearAllMocks()
})

describe('LoginPage authentication', () => {
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

  it('registers an account and then prompts the user to sign in', async () => {
    vi.mocked(register).mockResolvedValue({
      id: '7c9e6679-7425-40de-944b-e07fc1f90ae7',
      email: 'farmer@example.com',
    })

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Create an account' }))
    fireEvent.change(screen.getByLabelText('Email'), {
      target: { value: 'farmer@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'strong-password' } })
    fireEvent.change(screen.getByLabelText('Confirm password'), {
      target: { value: 'strong-password' },
    })
    expect(screen.getByRole('progressbar', { name: 'Password strength' })).toHaveAttribute(
      'aria-valuetext',
      'Medium',
    )
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Your account is ready. Sign in with your new credentials.',
    )
    expect(register).toHaveBeenCalledWith({
      email: 'farmer@example.com',
      password: 'strong-password',
    })
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument()
    expect(useAuthStore.getState().token).toBeNull()
    expect(screen.getByLabelText('Password')).toHaveValue('')
  })
})

describe('LoginPage registration validation', () => {
  it('does not register when the password confirmation does not match', async () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Create an account' }))
    fireEvent.change(screen.getByLabelText('Email'), {
      target: { value: 'farmer@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'strong-password' } })
    fireEvent.change(screen.getByLabelText('Confirm password'), {
      target: { value: 'different-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Passwords do not match.')
    expect(register).not.toHaveBeenCalled()
  })

  it('blocks weak passwords and allows medium-strength registration', async () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Create an account' }))
    fireEvent.change(screen.getByLabelText('Email'), {
      target: { value: 'farmer@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'weakpassword' } })
    fireEvent.change(screen.getByLabelText('Confirm password'), {
      target: { value: 'weakpassword' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Choose a medium or strong password to continue.',
    )
    expect(register).not.toHaveBeenCalled()
  })

  it('shows an inline warning when a registration password is too short', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Create an account' }))
    fireEvent.change(screen.getByLabelText('Email'), {
      target: { value: 'farmer@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'short' } })
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: 'short' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Use at least 8 characters for your password.',
    )
    expect(register).not.toHaveBeenCalled()
  })

  it('updates the password strength meter while registering', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Create an account' }))
    expect(screen.queryByRole('progressbar', { name: 'Password strength' })).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'abc' } })
    const strengthMeter = screen.getByRole('progressbar', { name: 'Password strength' })
    expect(strengthMeter).toHaveAttribute('aria-valuetext', 'Weak')

    fireEvent.change(screen.getByLabelText('Password'), {
      target: { value: 'StrongPassword1!' },
    })
    expect(strengthMeter).toHaveAttribute('aria-valuetext', 'Strong')
  })

  it('toggles visibility independently for each password field', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Create an account' }))
    fireEvent.change(screen.getByLabelText('Password'), {
      target: { value: 'sample-password' },
    })

    const passwordInput = screen.getByLabelText('Password')
    const confirmationInput = screen.getByLabelText('Confirm password')

    expect(passwordInput).toHaveAttribute('type', 'password')
    expect(confirmationInput).toHaveAttribute('type', 'password')

    const [showPasswordButton] = screen.getAllByRole('button', { name: 'Show password' })
    if (!showPasswordButton) {
      throw new Error('Password visibility button was not rendered.')
    }
    fireEvent.click(showPasswordButton)

    expect(passwordInput).toHaveAttribute('type', 'text')
    expect(confirmationInput).toHaveAttribute('type', 'password')
    expect(screen.getByRole('button', { name: 'Hide password' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
  })
})

describe('LoginPage authentication errors', () => {
  it('shows a friendly message when registration email is already in use', async () => {
    vi.mocked(register).mockRejectedValue(new ApiError(409, 'Request failed with status 409'))

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Create an account' }))
    fireEvent.change(screen.getByLabelText('Email'), {
      target: { value: 'farmer@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'strong-password' } })
    fireEvent.change(screen.getByLabelText('Confirm password'), {
      target: { value: 'strong-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'An account with this email already exists.',
    )
    expect(screen.queryByText(/status 409/i)).not.toBeInTheDocument()
  })

  it('shows a friendly message for a weak password response', async () => {
    vi.mocked(register).mockRejectedValue(new ApiError(422, 'Request failed with status 422'))

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Create an account' }))
    fireEvent.change(screen.getByLabelText('Email'), {
      target: { value: 'farmer@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'strong-password' } })
    fireEvent.change(screen.getByLabelText('Confirm password'), {
      target: { value: 'strong-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Enter a valid email address and a password with at least 8 characters.',
    )
    expect(screen.queryByText(/status 422/i)).not.toBeInTheDocument()
  })

  it('shows a friendly message for invalid login credentials', async () => {
    vi.mocked(login).mockRejectedValue(new ApiError(401, 'Request failed with status 401'))

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('Email'), {
      target: { value: 'farmer@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong-password' } })
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The email or password is incorrect.',
    )
    expect(screen.queryByText(/status 401/i)).not.toBeInTheDocument()
  })
})

describe('LoginPage session state', () => {
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
