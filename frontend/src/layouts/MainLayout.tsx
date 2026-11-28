import { NavLink, Outlet, useNavigate } from 'react-router'
import { useAuthStore } from '../features/auth/store/authStore'
import './MainLayout.css'

export function MainLayout() {
  const navigate = useNavigate()
  const clearToken = useAuthStore((state) => state.clear)

  function handleLogout() {
    clearToken()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <NavLink className="brand" to="/" aria-label="Rootine home">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 108 108" role="presentation">
              <path d="M16 86C15 45 42 12 102 0L17 86Z" fill="#fff" />
              <path d="M22 87C62 86 91 61 103 2L22 87Z" fill="#c5e1a5" />
              <path d="M18 84 0 105" fill="none" stroke="#fff" strokeWidth="6" />
            </svg>
          </span>
          <span>Rootine</span>
        </NavLink>
        <nav className="primary-nav" aria-label="Main navigation">
          <NavLink
            end
            className={({ isActive }) => `nav-link${isActive ? ' nav-link-active' : ''}`}
            to="/"
          >
            <span className="nav-indicator" aria-hidden="true" />
            Map
          </NavLink>
          <NavLink
            className={({ isActive }) => `nav-link${isActive ? ' nav-link-active' : ''}`}
            to="/risk"
          >
            <span className="nav-indicator" aria-hidden="true" />
            Risk
          </NavLink>
          <NavLink
            className={({ isActive }) => `nav-link${isActive ? ' nav-link-active' : ''}`}
            to="/diagnosis"
          >
            <span className="nav-indicator" aria-hidden="true" />
            Diagnosis
          </NavLink>
          <NavLink
            className={({ isActive }) => `nav-link${isActive ? ' nav-link-active' : ''}`}
            to="/alerts"
          >
            <span className="nav-indicator" aria-hidden="true" />
            Alerts
          </NavLink>
        </nav>
        <button className="sidebar-logout" type="button" onClick={handleLogout}>
          Sign out
        </button>
        <p className="sidebar-note">A little more care for every field.</p>
      </aside>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  )
}
