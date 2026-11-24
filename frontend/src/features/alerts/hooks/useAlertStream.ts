import { useEffect, useState } from 'react'
import { ApiError, openAlertStream, type AlertEvent } from '../../../lib/apiClient'

export type AlertStreamStatus = 'connecting' | 'connected' | 'disconnected' | 'error'

type AlertStreamState = {
  status: AlertStreamStatus
  alerts: AlertEvent[]
  errorMessage: string | null
}

const reconnectDelayMilliseconds = 3000
const maximumStoredAlerts = 100

function getStreamErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 501) {
    return 'Live alerts are not available yet. Please try again later.'
  }
  return 'We could not connect to live alerts. Please try again shortly.'
}

function parseServerSentEvents(buffer: string, onAlert: (alert: AlertEvent) => void): string {
  const normalizedBuffer = buffer.replace(/\r\n/g, '\n')
  const eventBlocks = normalizedBuffer.split('\n\n')
  const remainingBuffer = eventBlocks.pop() ?? ''

  for (const eventBlock of eventBlocks) {
    let eventName = 'message'
    const dataLines: string[] = []

    for (const line of eventBlock.split('\n')) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart())
      }
    }

    if (eventName === 'risk-alert' && dataLines.length > 0) {
      try {
        const alert = JSON.parse(dataLines.join('\n')) as AlertEvent
        onAlert(alert)
      } catch (error) {
        void error
      }
    }
  }

  return remainingBuffer
}

async function readAlertStream(
  response: Response,
  onAlert: (alert: AlertEvent) => void,
): Promise<void> {
  if (!response.body) {
    throw new Error('The alert stream returned no data.')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      return
    }
    buffer = parseServerSentEvents(buffer + decoder.decode(value, { stream: true }), onAlert)
  }
}

export function useAlertStream(): AlertStreamState {
  const [status, setStatus] = useState<AlertStreamStatus>('connecting')
  const [alerts, setAlerts] = useState<AlertEvent[]>([])
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    let reconnectTimer: number | undefined
    const abortController = new AbortController()

    const addAlert = (alert: AlertEvent) => {
      if (!active) return
      setAlerts((currentAlerts) => [alert, ...currentAlerts].slice(0, maximumStoredAlerts))
    }

    const scheduleReconnect = () => {
      if (!active) return
      reconnectTimer = window.setTimeout(() => {
        void connect()
      }, reconnectDelayMilliseconds)
    }

    const connect = async () => {
      if (!active) return
      setStatus('connecting')
      setErrorMessage(null)

      try {
        const response = await openAlertStream(abortController.signal)
        if (!active) return
        setStatus('connected')
        await readAlertStream(response, addAlert)
        if (active) {
          setStatus('disconnected')
          scheduleReconnect()
        }
      } catch (error) {
        if (!active) return
        setStatus('error')
        setErrorMessage(getStreamErrorMessage(error))
        if (!(error instanceof ApiError && error.status === 401)) {
          scheduleReconnect()
        }
      }
    }

    void connect()

    return () => {
      active = false
      abortController.abort()
      if (reconnectTimer !== undefined) {
        window.clearTimeout(reconnectTimer)
      }
    }
  }, [])

  return { status, alerts, errorMessage }
}
