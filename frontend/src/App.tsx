import { Route, Routes } from 'react-router'
import { MapView } from './features/maps/components/MapView'
import { DiagnosisPage } from './features/diagnosis/components/DiagnosisPage'
import { RiskView } from './features/weather/components/RiskView'
import { AlertsPage } from './features/alerts/components/AlertsPage'
import { MainLayout } from './layouts/MainLayout'

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<MapView />} />
        <Route path="/risk" element={<RiskView />} />
        <Route path="/diagnosis" element={<DiagnosisPage />} />
        <Route path="/alerts" element={<AlertsPage />} />
      </Route>
    </Routes>
  )
}
