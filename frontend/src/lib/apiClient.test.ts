import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../features/auth/store/authStore'
import { server } from '../test/msw/server'
import { ApiError, apiFetch, createParcel, getParcels } from './apiClient'

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

  it('gets parcels from the API', async () => {
    await expect(getParcels()).resolves.toMatchObject([
      { name: 'North field', crop: 'potato' },
      { name: 'South field', crop: 'wheat' },
    ])
  })

  it('creates a parcel with the contract shape', async () => {
    const parcel = await createParcel({
      name: 'East field',
      crop: 'tomato',
      geometry: {
        type: 'Polygon',
        coordinates: [
          [
            [23.59, 46.77],
            [23.6, 46.77],
            [23.6, 46.78],
            [23.59, 46.78],
            [23.59, 46.77],
          ],
        ],
      },
    })

    expect(parcel).toMatchObject({
      name: 'East field',
      crop: 'tomato',
      geometry: {
        type: 'Polygon',
        coordinates: [
          [
            [23.59, 46.77],
            [23.6, 46.77],
            [23.6, 46.78],
            [23.59, 46.78],
            [23.59, 46.77],
          ],
        ],
      },
    })
    expect(parcel.id).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
    )
    expect(parcel.createdAt).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/)
  })
})
