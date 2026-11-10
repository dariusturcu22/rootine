import { useState } from 'react'
import { useNavigate } from 'react-router'
import { ApiError, getDemoToken, getErrorMessage, login } from '../../../lib/apiClient'
import { useAuthStore } from '../store/authStore'
import './LoginPage.css'

export function LoginPage() {
  const navigate = useNavigate()
  const setToken = useAuthStore((state) => state.setToken)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function finishLogin(request: () => ReturnType<typeof login>) {
    setIsSubmitting(true)
    setError(null)

    try {
      const response = await request()
      setToken(response.accessToken)
      navigate('/', { replace: true })
    } catch (loginError) {
      if (loginError instanceof ApiError && loginError.status === 401) {
        setError('Your session could not be renewed. Please check your credentials and try again.')
      } else {
        setError(getErrorMessage(loginError, 'Unable to sign in. Please try again.'))
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formData = new FormData(event.currentTarget)

    void finishLogin(() =>
      login({
        email: String(formData.get('email') ?? ''),
        password: String(formData.get('password') ?? ''),
      }),
    )
  }

  return (
    <main className="login-page">
      <section className="login-card" aria-labelledby="login-title">
        <p className="eyebrow">WELCOME BACK</p>
        <h1 id="login-title">Sign in to Rootine</h1>
        <p className="login-description">Your session expired. Sign in to continue.</p>

        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            Email
            <input name="email" type="email" autoComplete="email" required />
          </label>
          <label>
            Password
            <input name="password" type="password" autoComplete="current-password" required />
          </label>
          {error && (
            <p className="login-error" role="alert">
              {error}
            </p>
          )}
          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        {import.meta.env.VITE_DEMO_MODE === 'true' && (
          <button
            className="login-demo-button"
            type="button"
            disabled={isSubmitting}
            onClick={() => void finishLogin(getDemoToken)}
          >
            Continue with local demo
          </button>
        )}
      </section>
    </main>
  )
}
