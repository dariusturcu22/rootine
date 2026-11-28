import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { ApiError, login, register, type LoginRequest } from '../../../lib/apiClient'
import { useAuthStore } from '../store/authStore'
import { LoginMapBackdrop } from './LoginMapBackdrop'
import { LoginPasswordInput } from './LoginPasswordInput'
import './LoginPage.css'

const PASSWORD_MIN_LENGTH = 8
const LONG_PASSWORD_LENGTH = 12
const PASSWORD_STRENGTH_MAX = 3

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setToken = useAuthStore((state) => state.setToken)
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const passwordStrength = getPasswordStrength(password)

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSuccess(null)

    if (mode === 'register' && password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    if (mode === 'register' && password.length < PASSWORD_MIN_LENGTH) {
      setError(`Use at least ${PASSWORD_MIN_LENGTH} characters for your password.`)
      return
    }

    if (mode === 'register' && passwordStrength.level === 'weak') {
      setError('Choose a medium or strong password to continue.')
      return
    }

    setIsSubmitting(true)
    const credentials: LoginRequest = { email, password }

    try {
      if (mode === 'register') {
        await register(credentials)
        setPassword('')
        setConfirmPassword('')
        setMode('login')
        setSuccess('Your account is ready. Sign in with your new credentials.')
        return
      }

      const response = await login(credentials)
      setToken(response.accessToken)
      navigate('/', { replace: true })
    } catch (authError) {
      setError(getAuthErrorMessage(authError, mode))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <LoginMapBackdrop />
      <section className="login-card" aria-labelledby="login-title">
        <p className="eyebrow">{mode === 'login' ? 'WELCOME BACK' : 'JOIN ROOTINE'}</p>
        <h1 id="login-title">{mode === 'login' ? 'Sign in to Rootine' : 'Create your account'}</h1>
        <p className="login-description">
          {mode === 'login' && location.state?.sessionExpired
            ? 'Your session expired. Sign in to continue.'
            : mode === 'login'
              ? 'Sign in to continue.'
              : 'Create an account to manage your fields and crop health.'}
        </p>

        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input
              name="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.currentTarget.value)}
            />
          </label>
          <label>
            Password
            <LoginPasswordInput
              id="login-password"
              name="password"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              value={password}
              onChange={(value) => {
                setPassword(value)
                setError(null)
              }}
            />
          </label>
          {mode === 'register' && password.length > 0 && (
            <>
              <div className="password-strength" data-strength={passwordStrength.level}>
                <div className="password-strength-heading">
                  <span>Password strength</span>
                  <span>{passwordStrength.label}</span>
                </div>
                <div
                  className="password-strength-track"
                  role="progressbar"
                  aria-label="Password strength"
                  aria-valuemin={0}
                  aria-valuemax={PASSWORD_STRENGTH_MAX}
                  aria-valuenow={passwordStrength.score}
                  aria-valuetext={passwordStrength.label}
                >
                  <span className="password-strength-fill" />
                </div>
                <p className="password-strength-hint">
                  {password.length < PASSWORD_MIN_LENGTH
                    ? `Use at least ${PASSWORD_MIN_LENGTH} characters.`
                    : 'A mix of character types makes passwords harder to guess.'}
                </p>
              </div>
              <label>
                Confirm password
                <LoginPasswordInput
                  id="confirm-password"
                  name="confirmPassword"
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(value) => {
                    setConfirmPassword(value)
                    setError(null)
                  }}
                />
              </label>
            </>
          )}
          {error && (
            <p className="login-error" role="alert">
              {error}
            </p>
          )}
          {success && (
            <p className="login-success" role="status">
              {success}
            </p>
          )}
          <button type="submit" disabled={isSubmitting}>
            {isSubmitting
              ? mode === 'login'
                ? 'Signing in...'
                : 'Creating account...'
              : mode === 'login'
                ? 'Sign in'
                : 'Create account'}
          </button>
        </form>

        <p className="login-mode-switch">
          {mode === 'login' ? 'New to Rootine?' : 'Already have an account?'}{' '}
          <button
            type="button"
            disabled={isSubmitting}
            onClick={() => {
              setMode(mode === 'login' ? 'register' : 'login')
              setPassword('')
              setConfirmPassword('')
              setError(null)
              setSuccess(null)
            }}
          >
            {mode === 'login' ? 'Create an account' : 'Sign in'}
          </button>
        </p>
      </section>
    </main>
  )
}

function getPasswordStrength(passwordValue: string): {
  level: 'empty' | 'weak' | 'medium' | 'strong'
  score: number
  label: string
} {
  if (!passwordValue) {
    return { level: 'empty', score: 0, label: 'Not entered' }
  }

  const criteriaScore = [
    passwordValue.length >= PASSWORD_MIN_LENGTH,
    passwordValue.length >= LONG_PASSWORD_LENGTH,
    /[a-z]/.test(passwordValue) && /[A-Z]/.test(passwordValue),
    /\d/.test(passwordValue),
    /[^A-Za-z0-9]/.test(passwordValue),
  ].filter(Boolean).length

  if (passwordValue.length < PASSWORD_MIN_LENGTH || criteriaScore <= 2) {
    return { level: 'weak', score: 1, label: 'Weak' }
  }
  if (criteriaScore === 3) {
    return { level: 'medium', score: 2, label: 'Medium' }
  }
  return { level: 'strong', score: 3, label: 'Strong' }
}

function getAuthErrorMessage(error: unknown, mode: 'login' | 'register'): string {
  if (error instanceof ApiError) {
    if (mode === 'register' && error.status === 409) {
      return 'An account with this email already exists. Sign in or use a different email.'
    }
    if (mode === 'login' && error.status === 401) {
      return 'The email or password is incorrect. Check your details and try again.'
    }
    if (error.status === 422) {
      return `Enter a valid email address and a password with at least ${PASSWORD_MIN_LENGTH} characters.`
    }
  }

  return mode === 'login'
    ? 'We could not sign you in right now. Please try again.'
    : 'We could not create your account right now. Please try again.'
}
