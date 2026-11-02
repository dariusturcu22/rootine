import { API_BASE_URL } from '../config/env'
import { useAuthStore } from '../features/auth/store/authStore'

export type DiagnosisStatus = 'confident' | 'unsure' | 'invalid_image' | 'crop_not_supported'

export interface Diagnosis
{
  id: string
  parcelId: string
  status: DiagnosisStatus
  crop: string
  cropCheck: 'ok' | 'mismatch' | null
  topPredictions: Array<{
    label: string
    disease: string
    probability: number
  }>
  supportLevel: 'lab_and_field_tested' | 'lab_only' | 'not_supported'
  modelVersion: string
  createdAt: string
}

export function createDiagnosis(image: File, parcelId: string): Promise<Diagnosis> 
{
  const formData = new FormData()
  formData.append('image', image)
  formData.append('parcelId', parcelId)

  return apiFetch<Diagnosis>('/diagnoses', {
    method: 'POST',
    body: formData,
  })
}

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/** fetch wrapper: prefixes the API base path, adds the JWT and parses JSON. */
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const token = useAuthStore.getState().token
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const url = new URL(`${API_BASE_URL}${path}`, window.location.href)
  const response = await fetch(url, { ...init, headers })
  if (!response.ok) {
    throw new ApiError(response.status, `Request failed with status ${response.status}`)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}
