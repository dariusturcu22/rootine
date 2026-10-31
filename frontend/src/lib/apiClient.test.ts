import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../features/auth/store/authStore'
import { server } from '../test/msw/server'
import { ApiError, apiFetch } from './apiClient'

describe('apiFetch', () => {
  afterEach(() => useAuthStore.getState().clear())

  it('adds the JWT and parses the JSON response', async () => {
    let authorization: string | null = null
    server.use(
      http.get('*/api/ping', ({ request }) => {
        authorization = request.headers.get('Authorization')
        return HttpResponse.json({ ok: true })
      }),
    )
    useAuthStore.getState().setToken('abc123')

    const body = await apiFetch<{ ok: boolean }>('/ping')

    expect(body).toEqual({ ok: true })
    expect(authorization).toBe('Bearer abc123')
  })

  it('sends no Authorization header when logged out', async () => {
    let authorization: string | null = 'unset'
    server.use(
      http.get('*/api/ping', ({ request }) => {
        authorization = request.headers.get('Authorization')
        return HttpResponse.json({})
      }),
    )

    await apiFetch('/ping')

    expect(authorization).toBeNull()
  })

  it('throws ApiError with the status for non-2xx responses', async () => {
    server.use(http.get('*/api/ping', () => new HttpResponse(null, { status: 401 })))

    await expect(apiFetch('/ping')).rejects.toMatchObject({ name: 'ApiError', status: 401 })
    await expect(apiFetch('/ping')).rejects.toBeInstanceOf(ApiError)
  })
})
