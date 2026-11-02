import { http, HttpResponse, type RequestHandler } from 'msw'
import type { Diagnosis } from '../../lib/apiClient'

/** Default handlers shared by all tests. Add per-test handlers with server.use(...). */
export const handlers: RequestHandler[] = [
    http.post('*/api/diagnoses', async ({request}) => {
        const formData = await request.formData()
        const image = formData.get('image')
        const parcelId = formData.get('parcelId')  
        
        if(!(image instanceof File) || typeof parcelId !== 'string') {
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
]
