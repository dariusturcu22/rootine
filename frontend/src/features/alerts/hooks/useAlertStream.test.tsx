import { cleanup, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../../auth/store/authStore'
import type { AlertEvent } from '../../../lib/apiClient'
import { useAlertStream } from './useAlertStream'

const alert = {
  parcelId: '3b241101-e2bb-4255-8caf-4136c566a962',
  parcelName: 'North field',
  crop: 'potato',
  level: 'high',
  riskIndex: 78,
  reason: 'High risk conditions were detected.',
  generatedAt: '2026-11-01T06:00:00Z',
} satisfies AlertEvent

beforeEach(() => {
  useAuthStore.getState().clear()
})

afterEach(() => {
  cleanup()
  useAuthStore.getState().clear()
  vi.restoreAllMocks()
})

describe('useAlertStream', () => {
  it('receives a risk-alert event from the authenticated SSE stream', async () => {
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(
          new TextEncoder().encode(`event: risk-alert\r\ndata: ${JSON.stringify(alert)}\r`),
        )
        controller.enqueue(new TextEncoder().encode('\n\r\n'))
        controller.close()
      },
    })
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(stream, { headers: { 'Content-Type': 'text/event-stream' } }),
    )
    useAuthStore.getState().setToken('test-token')

    const { result, unmount } = renderHook(() => useAlertStream())

    await waitFor(() => {
      expect(result.current.alerts).toEqual([alert])
    })

    const request = fetchMock.mock.calls[0]?.[1]
    expect(new Headers(request?.headers).get('Authorization')).toBe('Bearer test-token')
    unmount()
  })

  it('shows a friendly message when the alert stream is not implemented', async () => {
    const technicalDetail = 'This endpoint is published in the API contract but is not implemented yet.'
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ detail: technicalDetail }), {
        status: 501,
        headers: { 'Content-Type': 'application/problem+json' },
      }),
    )

    const { result, unmount } = renderHook(() => useAlertStream())

    await waitFor(() => {
      expect(result.current.errorMessage).toBe(
        'Live alerts are not available yet. Please try again later.',
      )
    })
    expect(result.current.errorMessage).not.toContain(technicalDetail)
    unmount()
  })
})
