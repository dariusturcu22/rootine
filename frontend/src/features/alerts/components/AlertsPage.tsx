import { Link } from 'react-router'
import { useAlertStream, type AlertStreamStatus } from '../hooks/useAlertStream'
import type { AlertEvent } from '../../../lib/apiClient'
import './AlertsPage.css'

function getStreamBadge(status: AlertStreamStatus): string {
  if (status === 'connected') return 'Live'
  if (status === 'connecting') return 'Connecting'
  if (status === 'error') return 'Stream unavailable'
  return 'Stream disconnected'
}

function formatAlertTime(generatedAt: string | undefined): string {
  if (!generatedAt) return 'Time unavailable'
  const date = new Date(generatedAt)
  if (Number.isNaN(date.getTime())) return 'Time unavailable'
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function AlertItem({ alert }: { alert: AlertEvent }) {
  const parcelName = alert.parcelName?.trim() || 'Unnamed parcel'
  const crop = alert.crop?.trim() || 'Crop unavailable'
  const riskIndex = typeof alert.riskIndex === 'number' ? `${alert.riskIndex}/100` : 'N/A'
  const reason = alert.reason?.trim() || 'High risk conditions were detected.'

  return (
    <article className="alerts-feed-item">
      <div className="alerts-risk-indicator" aria-hidden="true" />
      <div className="alerts-feed-content">
        <div className="alerts-feed-heading">
          <div>
            <h3>{parcelName}</h3>
            <p>
              {crop} <span aria-hidden="true">·</span> {riskIndex}
            </p>
          </div>
          <time dateTime={alert.generatedAt}>{formatAlertTime(alert.generatedAt)}</time>
        </div>
        <p className="alerts-feed-reason">{reason}</p>
      </div>
    </article>
  )
}

export function AlertsPage() {
  const { status, alerts, errorMessage } = useAlertStream()
  const streamBadgeClass =
    status === 'connected' ? 'alerts-live-badge-connected' : 'alerts-live-badge-pending'

  return (
    <section className="alerts-page">
      <header className="page-heading alerts-heading">
        <div>
          <p className="eyebrow">STAY IN THE LOOP</p>
          <h1>Alerts</h1>
          <p className="page-description">
            Important field updates when high disease risk is detected.
          </p>
        </div>
        <span className={`alerts-live-badge ${streamBadgeClass}`} role="status">
          <span aria-hidden="true" />
          {getStreamBadge(status)}
        </span>
      </header>

      <div className="alerts-panel">
        <div className="alerts-panel-heading">
          <div>
            <h2>Recent alerts</h2>
          </div>
          {alerts.length > 0 && (
            <p>
              {alerts.length} live {alerts.length === 1 ? 'alert' : 'alerts'}
            </p>
          )}
        </div>

        {alerts.length > 0 ? (
          <div className="alerts-feed" role="region" aria-label="Live risk alerts">
            {alerts.map((alert, index) => (
              <AlertItem
                key={`${alert.parcelId ?? 'parcel'}-${alert.generatedAt ?? index}`}
                alert={alert}
              />
            ))}
          </div>
        ) : (
          <div className="alerts-empty-state" role="status">
            {status === 'error' ? (
              <>
                <h3>Alerts could not be loaded</h3>
                <p>{errorMessage}</p>
              </>
            ) : status === 'connected' ? (
              <>
                <h3>No new alerts</h3>
                <p>High-risk alerts will appear here as they arrive.</p>
              </>
            ) : (
              <>
                <h3>Connecting to live alerts</h3>
                <p>The alert stream is being established.</p>
              </>
            )}
            <Link className="alerts-risk-link" to="/risk">
              View field risk
            </Link>
          </div>
        )}
      </div>

      <p className="alerts-retention-note">Alerts stay here for this session only.</p>
    </section>
  )
}
