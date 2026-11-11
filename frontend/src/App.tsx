import { useEffect } from 'react'
import { Route, Routes, useNavigate } from 'react-router'
import { MapView } from './features/maps/components/MapView'
import { DiagnosisPage } from './features/diagnosis/components/DiagnosisPage'
import { RiskView } from './features/weather/components/RiskView'
import { AlertsPage } from './features/alerts/components/AlertsPage'
import { LoginPage } from './features/auth/components/LoginPage'
import { MainLayout } from './layouts/MainLayout'
import { SESSION_EXPIRED_EVENT } from './lib/apiClient'

export default function App() {
  const navigate = useNavigate()

  useEffect(() => {
    const handleSessionExpired = () =>
      navigate('/login', { replace: true, state: { sessionExpired: true } })
    window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)

    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)
  }, [navigate])

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<MainLayout />}>
        <Route index element={<MapView />} />
        <Route path="/risk" element={<RiskView />} />
        <Route path="/diagnosis" element={<DiagnosisPage />} />
        <Route path="/alerts" element={<AlertsPage />} />
      </Route>
    </Routes>
  )
}
