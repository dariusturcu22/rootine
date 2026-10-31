import { Route, Routes } from 'react-router'
import { MapView } from './features/maps/components/MapView'
import { MainLayout } from './layouts/MainLayout'

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<MapView />} />
      </Route>
    </Routes>
  )
}
