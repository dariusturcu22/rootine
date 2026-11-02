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
    id: '7f4e3a9d-1c2b-4e8f-9a6d-5b0c1d2e3f40',
    name: 'South field',
    crop: 'wheat',
    geometry: {
      type: 'Polygon',
      coordinates: [
        [
          [24.95, 44.41],
          [24.97, 44.41],
          [24.97, 44.42],
          [24.95, 44.42],
          [24.95, 44.41],
        ],
      ],
    },
    createdAt: '2026-10-31T08:00:00Z',
  },
]

function findParcel(id: string): Parcel | undefined {
  return parcels.find((parcel) => parcel.id === id)
}

/** Default handlers shared by tests and the local development browser worker. */
export const handlers: RequestHandler[] = [
  http.get('*/api/parcels', () => HttpResponse.json(parcels)),
  http.get('*/api/parcels/:id', ({ params }) => {
    const parcel = findParcel(String(params.id))
    return parcel ? HttpResponse.json(parcel) : new HttpResponse(null, { status: 404 })
  }),
  http.post('*/api/parcels', async ({ request }) => {
    const input = (await request.json()) as ParcelInput
    const parcel: Parcel = {
      ...input,
      id: crypto.randomUUID(),
      createdAt: new Date().toISOString(),
    }
    parcels.unshift(parcel)
    return HttpResponse.json(parcel, { status: 201 })
  }),
  http.put('*/api/parcels/:id', async ({ params, request }) => {
    const index = parcels.findIndex((parcel) => parcel.id === String(params.id))
    if (index === -1) return new HttpResponse(null, { status: 404 })

    const updated: Parcel = {
      ...((await request.json()) as ParcelInput),
      id: parcels[index].id,
      createdAt: parcels[index].createdAt,
    }
    parcels[index] = updated
    return HttpResponse.json(updated)
  }),
  http.delete('*/api/parcels/:id', ({ params }) => {
    const index = parcels.findIndex((parcel) => parcel.id === String(params.id))
    if (index === -1) return new HttpResponse(null, { status: 404 })

    parcels.splice(index, 1)
    return new HttpResponse(null, { status: 204 })
  }),
]
