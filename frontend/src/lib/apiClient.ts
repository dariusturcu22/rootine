import { API_BASE_URL } from '../config/env'
import { useAuthStore } from '../features/auth/store/authStore'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export interface ParcelGeometry {
  type: 'Polygon'
  coordinates: [number, number][][]
}

export interface Parcel {
  id: string
  name: string
  crop: string
  geometry: ParcelGeometry
  createdAt: string
}

export interface ParcelInput {
  name: string
  crop: string
  geometry: ParcelGeometry
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
