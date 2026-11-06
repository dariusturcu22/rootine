import { http, HttpResponse, type RequestHandler } from 'msw'
import type { Crop, Diagnosis, Parcel, ParcelInput } from '../../lib/apiClient'

const parcels: Parcel[] = [
  {
    id: '3b241101-e2bb-4255-8caf-4136c566a962',
    name: 'North field',
    crop: 'potato',
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
    createdAt: '2026-11-01T08:00:00Z',
  },
  {
    id: '7c9e6679-7425-40de-944b-e07fc1f90ae7',
    name: 'South field',
    crop: 'tomato',
    geometry: {
      type: 'Polygon',
      coordinates: [
        [
          [24.1, 46.5],
          [24.11, 46.5],
          [24.11, 46.51],
          [24.1, 46.51],
          [24.1, 46.5],
        ],
      ],
    },
    createdAt: '2026-11-02T06:00:00Z',
  },
]

const problem = (status: number, title: string, detail: string) =>
  HttpResponse.json(
    { type: 'about:blank', title, status, detail },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )

const crops: Crop[] = [
  {
    crop: 'potato',
    diagnosisSupported: true,
    weatherSupported: true,
    supportLevel: 'lab_only',
    weatherRules: ['hutton'],
    classes: ['early_blight', 'healthy', 'late_blight'],
  },
  {
    crop: 'tomato',
    diagnosisSupported: true,
    weatherSupported: false,
    supportLevel: 'lab_only',
    weatherRules: [],
    classes: ['healthy'],
  },
]

function isValidGeometry(geometry: ParcelInput['geometry']): boolean {
  const ring = geometry?.coordinates?.[0]
  if (!ring || ring.length < 4) return false

  const first = ring[0]
  const last = ring.at(-1)
  if (!first || !last || first[0] !== last[0] || first[1] !== last[1]) {
    return false
  }

  return ring.every(
    ([longitude, latitude]) =>
      Number.isFinite(longitude) &&
      Number.isFinite(latitude) &&
      longitude >= -180 &&
      longitude <= 180 &&
      latitude >= -90 &&
      latitude <= 90,
  )
}

/** Default handlers shared by all tests and the local development browser. */
export const handlers: RequestHandler[] = [
  http.post('*/api/auth/demo-token', () =>
    HttpResponse.json({ accessToken: 'demo-access-token', tokenType: 'Bearer', expiresIn: 3600 }),
  ),
  http.get('*/api/crops', () => HttpResponse.json(crops)),

  http.post('*/api/diagnoses', async ({ request }) => {
    const formData = await request.formData()
    const image = formData.get('image')
    const parcelId = formData.get('parcelId')

    if (!(image instanceof File) || typeof parcelId !== 'string') {
      return HttpResponse.json(
        {
          type: 'about:blank',
          title: 'Bad Request',
          status: 400,
          detail: 'image and parcelId are required',
        },
        { status: 400 },
      )
    }

    const diagnosis: Diagnosis = {
      id: '9a1c7f52-6b3e-4c1a-9d0e-1f2a3b4c5d6e',
      parcelId,
      status: 'confident',
      crop: 'potato',
      cropCheck: 'ok',
      topPredictions: [
        {
          label: 'potato_early_blight',
          disease: 'early_blight',
          probability: 0.92,
        },
      ],
      supportLevel: 'lab_only',
      modelVersion: 'plant-v1',
      createdAt: '2026-11-01T08:05:00Z',
    }

    return HttpResponse.json(diagnosis, { status: 201 })
  }),

  http.get('*/api/parcels', () =>
    HttpResponse.json([...parcels].sort((a, b) => b.createdAt.localeCompare(a.createdAt))),
  ),
  http.post('*/api/parcels', async ({ request }) => {
    const body = (await request.json()) as ParcelInput
    if (
      !body.name?.trim() ||
      body.name.length > 100 ||
      !body.crop ||
      body.geometry?.type !== 'Polygon' ||
      !isValidGeometry(body.geometry)
    ) {
      return problem(422, 'Unprocessable Entity', 'The parcel body is invalid.')
    }

    const parcel: Parcel = {
      id: crypto.randomUUID(),
      name: body.name,
      crop: body.crop,
      geometry: body.geometry,
      createdAt: new Date().toISOString(),
    }
    parcels.push(parcel)
    return HttpResponse.json(parcel, { status: 201 })
  }),
  http.get('*/api/parcels/:id', ({ params }) => {
    const parcel = parcels.find(({ id }) => id === params.id)
    return parcel ? HttpResponse.json(parcel) : problem(404, 'Not Found', 'Parcel not found.')
  }),
  http.put('*/api/parcels/:id', async ({ params, request }) => {
    const index = parcels.findIndex(({ id }) => id === params.id)
    if (index < 0) return problem(404, 'Not Found', 'Parcel not found.')

    const body = (await request.json()) as ParcelInput
    if (
      !body.name?.trim() ||
      body.name.length > 100 ||
      !body.crop ||
      body.geometry?.type !== 'Polygon' ||
      !isValidGeometry(body.geometry)
    ) {
      return problem(422, 'Unprocessable Entity', 'The parcel body is invalid.')
    }

    parcels[index] = { ...parcels[index], ...body }
    return HttpResponse.json(parcels[index])
  }),
  http.delete('*/api/parcels/:id', ({ params }) => {
    const index = parcels.findIndex(({ id }) => id === params.id)
    if (index < 0) return problem(404, 'Not Found', 'Parcel not found.')
    parcels.splice(index, 1)
    return new HttpResponse(null, { status: 204 })
  }),
]
