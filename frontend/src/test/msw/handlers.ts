import { http, HttpResponse, type RequestHandler } from 'msw'
import type { Parcel, ParcelInput } from '../../lib/apiClient'

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

/** Default handlers shared by all tests and the local development browser. */
export const handlers: RequestHandler[] = [
  http.get('*/api/parcels', () =>
    HttpResponse.json([...parcels].sort((a, b) => b.createdAt.localeCompare(a.createdAt))),
  ),
  http.post('*/api/parcels', async ({ request }) => {
    const body = (await request.json()) as ParcelInput
    if (!body.name?.trim() || !body.crop || body.geometry?.type !== 'Polygon') {
      return problem(422, 'Unprocessable Entity', 'The parcel body is invalid.')
    }

    const parcel: Parcel = {
      ...body,
      id: crypto.randomUUID(),
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
    if (!body.name?.trim() || !body.crop || body.geometry?.type !== 'Polygon') {
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
