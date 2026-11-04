import { Route, Routes } from 'react-router'
import { MapView } from './features/maps/components/MapView'
import { ImageUpload } from './features/diagnosis/components/ImageUpload'
import { MainLayout } from './layouts/MainLayout'

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<MapView />} />
        <Route path="/diagnosis" element={<ImageUpload />} />
      </Route>
    </Routes>
  )
}
