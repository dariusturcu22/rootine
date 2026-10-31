/** Base path of the backend API. nginx and the Vite dev proxy both map it to the backend. */
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? '/api'

/** Center of Romania, used as the initial map view. */
export const MAP_CENTER: [number, number] = [45.9432, 24.9668]
export const MAP_ZOOM = 6
