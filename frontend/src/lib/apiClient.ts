import { API_BASE_URL } from '../config/env'
import { useAuthStore } from '../features/auth/store/authStore'

export type DiagnosisStatus = 'confident' | 'unsure' | 'invalid_image' | 'crop_not_supported'

export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
}

export interface RegisterResponse {
  id: string
  email: string
}

export interface User {
  id: string
  email: string
}

export function register(email: string, password: string): Promise<RegisterResponse> {
  return apiFetch<RegisterResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export function login(email: string, password: string): Promise<AuthResponse> {
  return apiFetch<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export function getMe(): Promise<User> {
  return apiFetch<User>('/me')
}

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

export function getDiagnoses(parcelId?: string): Promise<Diagnosis[]> {
  const query = parcelId ? `?parcelId=${encodeURIComponent(parcelId)}` : ''
  return apiFetch<Diagnosis[]>(`/diagnoses${query}`)
}

export function getDiagnosis(id: string): Promise<Diagnosis> {
  return apiFetch<Diagnosis>(`/diagnoses/${id}`)
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

export interface RiskDisease {
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
  diseases: RiskDisease[]
  generatedAt: string
}

export interface HeatmapPoint {
  parcelId: string
  name: string
  crop: string
  latitude: number
  longitude: number
  level: RiskLevel
  riskIndex: number
  generatedAt: string
}

export interface RiskAlert {
  parcelId: string
  parcelName: string
  crop: string
  level: 'high'
  riskIndex: number
  reason: string
  generatedAt: string
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const token = useAuthStore.getState().token
  const isPublicAuthRequest = path === '/auth/login' || path === '/auth/register'
  if (token && !isPublicAuthRequest) {
    headers.set('Authorization', ['Bearer', token].join(' '))
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

export function getRiskHeatmap(): Promise<HeatmapPoint[]> {
  return apiFetch<HeatmapPoint[]>('/risk/heatmap')
}

export async function openAlertsStream(
  onAlert: (alert: RiskAlert) => void,
  signal?: AbortSignal,
): Promise<void> {
  const headers = new Headers({ Accept: 'text/event-stream' })
  const token = useAuthStore.getState().token
  if (token) {
    headers.set('Authorization', ['Bearer', token].join(' '))
  }

  const url = new URL(`${API_BASE_URL}/alerts/stream`, window.location.href)
  const response = await fetch(url, { headers, signal })
  if (!response.ok) {
    throw new ApiError(response.status, `Request failed with status ${response.status}`)
  }
  if (!response.body) {
    throw new Error('The alerts stream returned no response body.')
  }

  const reader = response.body.pipeThrough(new TextDecoderStream()).getReader()
  let buffer = ''
  let eventName = ''
  let eventData: string[] = []

  const dispatchEvent = () => {
    if (eventName === 'risk-alert' && eventData.length > 0) {
      onAlert(JSON.parse(eventData.join('\n')) as RiskAlert)
    }
    eventName = ''
    eventData = []
  }

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += value

      const lines = buffer.split(/\r?\n/)
      buffer = lines.pop() ?? ''
      for (const line of lines) {
        if (line === '') {
          dispatchEvent()
        } else if (line.startsWith('event:')) {
          eventName = line.slice('event:'.length).trim()
        } else if (line.startsWith('data:')) {
          eventData.push(line.slice('data:'.length).trim())
        }
      }
    }
    if (buffer === '') dispatchEvent()
  } finally {
    reader.releaseLock()
  }
}
