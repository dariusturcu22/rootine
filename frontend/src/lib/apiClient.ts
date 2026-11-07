import { API_BASE_URL } from '../config/env'
import { useAuthStore } from '../features/auth/store/authStore'

export type DiagnosisStatus = 'confident' | 'unsure' | 'invalid_image' | 'crop_not_supported'

export interface Diagnosis {
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

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
}

export function getDemoToken(): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/auth/demo-token', { method: 'POST' })
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

export interface Crop {
  crop: string
  diagnosisSupported: boolean
  weatherSupported: boolean
  supportLevel: 'lab_and_field_tested' | 'lab_only' | 'not_supported'
  weatherRules: string[]
  classes: string[]
}

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

export type Position = [number, number]

export type ParcelGeometry = {
  type: 'Polygon'
  coordinates: Position[][]
}

export type Parcel = {
  id: string
  name: string
  crop: string
  geometry: ParcelGeometry
  createdAt: string
}

export type ParcelInput = Omit<Parcel, 'id' | 'createdAt'>

export type RiskLevel = 'low' | 'medium' | 'high' | 'not_modelled'

export interface DiseaseRisk {
  disease: string
  model: string
  level: RiskLevel
  riskIndex: number
}

export interface ParcelRisk {
  parcelId: string
  crop: string
  level: RiskLevel
  riskIndex: number
  reason: string
  diseases: DiseaseRisk[]
  generatedAt: string
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
