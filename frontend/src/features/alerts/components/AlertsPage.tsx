import { Link } from 'react-router'
import './AlertsPage.css'

export function AlertsPage() {
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
        <span className="alerts-live-badge alerts-live-badge-pending">
          <span aria-hidden="true" />
          Stream not connected
        </span>
      </header>

      <div className="alerts-panel">
        <div className="alerts-panel-heading">
          <div>
            <h2>Recent alerts</h2>
          </div>
        </div>

        <div className="alerts-empty-state" role="status">
          <h3>Live alerts are not connected yet</h3>
          <p>When connected, high-risk alerts will appear here.</p>
          <Link className="alerts-risk-link" to="/risk">
            View field risk
          </Link>
        </div>
      </div>

      <p className="alerts-retention-note">No alerts in demo1.</p>
    </section>
  )
}
