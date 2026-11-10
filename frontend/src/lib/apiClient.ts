import { API_BASE_URL } from '../config/env'
import { useAuthStore } from '../features/auth/store/authStore'
import type { components } from '../types/api'

type RequiredSchema<T> = { [K in keyof T]-?: T[K] }

export type Diagnosis = RequiredSchema<
  Omit<components['schemas']['DiagnosisResponse'], 'topPredictions'>
> & {
  topPredictions: Array<RequiredSchema<components['schemas']['Prediction']>>
}
export type DiagnosisStatus = NonNullable<Diagnosis['status']>

export type LoginResponse = RequiredSchema<components['schemas']['LoginResponse']>
export type RegisterRequest = components['schemas']['RegisterRequest']
export type LoginRequest = components['schemas']['LoginRequest']
export type User = RequiredSchema<components['schemas']['UserResponse']>

export function getDemoToken(): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/auth/demo-token', { method: 'POST' })
}

export function register(request: RegisterRequest): Promise<User> {
  return apiFetch<User>('/auth/register', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function login(request: LoginRequest): Promise<LoginResponse> {
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

export type Crop = components['schemas']['CropResponse']

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

export type Position = components['schemas']['PolygonGeometry']['coordinates'][number][number]
export type ParcelGeometry = components['schemas']['PolygonGeometry']
export type Parcel = RequiredSchema<components['schemas']['ParcelResponse']>
export type ParcelInput = components['schemas']['ParcelRequest']
export type RiskLevel = NonNullable<components['schemas']['RiskResponse']['level']>
export type DiseaseRisk = RequiredSchema<components['schemas']['DiseaseRisk']>
export type ParcelRisk = RequiredSchema<
  Omit<components['schemas']['RiskResponse'], 'diseases'>
> & {
  diseases: Array<DiseaseRisk>
}
export type HeatmapPoint = RequiredSchema<components['schemas']['HeatmapPoint']>
export type AlertEvent = RequiredSchema<components['schemas']['AlertEvent']>

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

export function openAlertStream(): Promise<Response> {
  const headers = new Headers()
  const token = useAuthStore.getState().token
  if (token) {
    headers.set('Authorization', ['Bearer', token].join(' '))
  }

  const url = new URL(`${API_BASE_URL}/alerts/stream`, window.location.href)
  return fetch(url, { headers }).then((response) => {
    if (!response.ok) {
      throw new ApiError(response.status, `Request failed with status ${response.status}`)
    }
    return response
  })
}
