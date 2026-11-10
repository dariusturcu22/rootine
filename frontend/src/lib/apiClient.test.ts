import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../features/auth/store/authStore'
import { server } from '../test/msw/server'
import {
  ApiError,
  apiFetch,
  createParcel,
  getDemoToken,
  getParcels,
  login,
  openAlertStream,
  SESSION_EXPIRED_EVENT,
} from './apiClient'

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

  it('gets the local demo access token', async () => {
    const token = await getDemoToken()

    expect(token).toEqual({
      accessToken: 'demo-access-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
    })
  })

  it('throws ApiError with the status for non-2xx responses', async () => {
    server.use(http.get('*/api/ping', () => new HttpResponse(null, { status: 401 })))
    useAuthStore.getState().setToken('expired-token')
    const dispatchEvent = vi.spyOn(window, 'dispatchEvent')

    await expect(apiFetch('/ping')).rejects.toMatchObject({ name: 'ApiError', status: 401 })
    await expect(apiFetch('/ping')).rejects.toBeInstanceOf(ApiError)
    expect(useAuthStore.getState().token).toBeNull()
    expect(dispatchEvent).toHaveBeenCalledWith(
      expect.objectContaining({ type: SESSION_EXPIRED_EVENT }),
    )
  })

  it('clears the token and raises the session-expired event for a 401 alert stream response', async () => {
    server.use(http.get('*/api/alerts/stream', () => new HttpResponse(null, { status: 401 })))
    useAuthStore.getState().setToken('expired-token')
    const dispatchEvent = vi.spyOn(window, 'dispatchEvent')

    await expect(openAlertStream()).rejects.toMatchObject({ name: 'ApiError', status: 401 })

    expect(useAuthStore.getState().token).toBeNull()
    expect(dispatchEvent).toHaveBeenCalledWith(
      expect.objectContaining({ type: SESSION_EXPIRED_EVENT }),
    )
  })

  it('does not expire an existing session for invalid login credentials', async () => {
    server.use(http.post('*/api/auth/login', () => new HttpResponse(null, { status: 401 })))
    useAuthStore.getState().setToken('current-token')
    const dispatchEvent = vi.spyOn(window, 'dispatchEvent')

    await expect(
      login({ email: 'farmer@example.com', password: 'wrong-password' }),
    ).rejects.toMatchObject({
      status: 401,
    })

    expect(useAuthStore.getState().token).toBe('current-token')
    expect(dispatchEvent).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: SESSION_EXPIRED_EVENT }),
    )
  })

  it('fetches parcels in the backend contract shape from the MSW handler', async () => {
    const parcels = await getParcels()

    expect(parcels.length).toBeGreaterThan(0)
    expect(parcels[0]).toMatchObject({
      id: expect.stringMatching(
        /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/,
      ),
      name: expect.any(String),
      crop: expect.any(String),
      geometry: {
        type: 'Polygon',
        coordinates: expect.arrayContaining([
          expect.arrayContaining([
            expect.arrayContaining([expect.any(Number), expect.any(Number)]),
          ]),
        ]),
      },
      createdAt: expect.stringMatching(/Z$/),
    })
  })

  it('creates a parcel through the MSW handler', async () => {
    const parcel = await createParcel({
      name: 'Test field',
      crop: 'potato',
      geometry: {
        type: 'Polygon',
        coordinates: [
          [
            [23.7, 46.7],
            [23.71, 46.7],
            [23.71, 46.71],
            [23.7, 46.71],
            [23.7, 46.7],
          ],
        ],
      },
    })

    expect(parcel).toMatchObject({
      id: expect.any(String),
      name: 'Test field',
      crop: 'potato',
      createdAt: expect.stringMatching(/Z$/),
    })
    expect(parcel.geometry.coordinates[0][0]).toEqual([23.7, 46.7])
  })
})
