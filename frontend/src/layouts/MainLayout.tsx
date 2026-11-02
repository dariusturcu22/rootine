import { Outlet, Link } from 'react-router'

export function MainLayout() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <header style={{ padding: '0.75rem 1rem', borderBottom: '1px solid #ddd' }}>
        <strong>Rootine</strong>
        <Link to = "/diagnosis">Diagnose a leaf</Link>
      </header>
      <main style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
        <Outlet />
      </main>
    </div>
  )
}
