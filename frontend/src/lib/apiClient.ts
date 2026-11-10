import { API_BASE_URL } from '../config/env'
import { useAuthStore } from '../features/auth/store/authStore'
import type { components } from '../types/api'

type RequiredSchema<K extends keyof components['schemas']> = Required<components['schemas'][K]>

export type DiagnosisStatus = NonNullable<components['schemas']['DiagnosisResponse']['status']>
type Prediction = RequiredSchema<'Prediction'>
export type Diagnosis = Omit<RequiredSchema<'DiagnosisResponse'>, 'crop' | 'topPredictions'> & {
  crop: string | null
  topPredictions: Prediction[]
}

export type LoginResponse = RequiredSchema<'LoginResponse'>
export type RegisterRequest = components['schemas']['RegisterRequest']
export type User = RequiredSchema<'UserResponse'>

export function getDemoToken(): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/auth/demo-token', { method: 'POST' })
}

export function register(request: RegisterRequest): Promise<User> {
  return apiFetch<User>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function login(request: RegisterRequest): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function getMe(): Promise<User> {
  return apiFetch<User>('/me')
}

export function createDiagnosis(image: File, parcelId: string): Promise<Diagnosis> {
  const formData = new FormData()
  formData.append('image', image)
  formData.append('parcelId', parcelId)

  return apiFetch<Diagnosis>('/diagnoses', {
    method: 'POST',
    body: formData,
  })
}

export type Crop = RequiredSchema<'CropResponse'>

export function getCrops(): Promise<Crop[]> {
  return apiFetch<Crop[]>('/crops')
}

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

async function getApiErrorMessage(response: Response): Promise<string> {
  const responseBody = await response.text()

  if (responseBody) {
    try {
      const problem = JSON.parse(responseBody) as { detail?: unknown }
      if (typeof problem.detail === 'string' && problem.detail) {
        return problem.detail
      }
    } catch (error) {
      if (!(error instanceof SyntaxError)) {
        throw error
      }
    }
  }

  return `Request failed with status ${response.status}`
}

export const SESSION_EXPIRED_EVENT = 'rootine:session-expired'

const publicAuthPaths = new Set(['/auth/register', '/auth/login', '/auth/demo-token'])

function handleUnauthorized(path: string): void {
  if (publicAuthPaths.has(path)) {
    return
  }

  useAuthStore.getState().clear()
  window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
}

export type Position = RequiredSchema<'PolygonGeometry'>['coordinates'][number][number]
export type ParcelGeometry = RequiredSchema<'PolygonGeometry'>
export type Parcel = RequiredSchema<'ParcelResponse'>
export type ParcelInput = components['schemas']['ParcelRequest']
export type RiskLevel = NonNullable<components['schemas']['RiskResponse']['level']>
export type DiseaseRisk = RequiredSchema<'DiseaseRisk'>
export type ParcelRisk = Omit<RequiredSchema<'RiskResponse'>, 'diseases'> & {
  diseases: DiseaseRisk[]
}
export type HeatmapPoint = RequiredSchema<'HeatmapPoint'>
export type AlertEvent = RequiredSchema<'AlertEvent'>

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
    if (response.status === 401) {
      handleUnauthorized(path)
    }
    throw new ApiError(response.status, await getApiErrorMessage(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export function getParcels(): Promise<Parcel[]> {
  return apiFetch<Parcel[]>('/parcels')
}

export function getParcel(id: string): Promise<Parcel> {
  return apiFetch<Parcel>(`/parcels/${id}`)
}

export function createParcel(parcel: ParcelInput): Promise<Parcel> {
  return apiFetch<Parcel>('/parcels', {
    method: 'POST',
    body: JSON.stringify(parcel),
  })
}

export function updateParcel(id: string, parcel: ParcelInput): Promise<Parcel> {
  return apiFetch<Parcel>(`/parcels/${id}`, {
    method: 'PUT',
    body: JSON.stringify(parcel),
  })
}

export function deleteParcel(id: string): Promise<void> {
  return apiFetch<void>(`/parcels/${id}`, { method: 'DELETE' })
}

export function getParcelRisk(id: string): Promise<ParcelRisk> {
  return apiFetch<ParcelRisk>(`/parcels/${id}/risk`)
}

export function getHeatmap(): Promise<HeatmapPoint[]> {
  return apiFetch<HeatmapPoint[]>('/risk/heatmap')
}

export function getDiagnoses(parcelId?: string): Promise<Diagnosis[]> {
  const query = parcelId ? `?parcelId=${encodeURIComponent(parcelId)}` : ''
  return apiFetch<Diagnosis[]>(`/diagnoses${query}`)
}

export function getDiagnosis(id: string): Promise<Diagnosis> {
  return apiFetch<Diagnosis>(`/diagnoses/${id}`)
}

export async function openAlertStream(): Promise<Response> {
  const headers = new Headers()
  const token = useAuthStore.getState().token
  if (token) {
    headers.set('Authorization', ['Bearer', token].join(' '))
  }

  const url = new URL(`${API_BASE_URL}/alerts/stream`, window.location.href)
  const response = await fetch(url, { headers })
  if (!response.ok) {
    if (response.status === 401) {
      handleUnauthorized('/alerts/stream')
    }
    throw new ApiError(response.status, await getApiErrorMessage(response))
  }
  return response
}
