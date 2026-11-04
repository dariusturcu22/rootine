import { Route, Routes } from 'react-router'
import { MapView } from './features/maps/components/MapView'
import { DiagnosisPage } from './features/diagnosis/components/DiagnosisPage'
import { MainLayout } from './layouts/MainLayout'

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<MapView />} />
        <Route path="/diagnosis" element={<DiagnosisPage />} />
      </Route>
    </Routes>
  )
}
