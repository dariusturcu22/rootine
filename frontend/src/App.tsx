import { Route, Routes } from 'react-router'
import { MapView } from './features/maps/components/MapView'
import { ImageUpload } from './features/diagnosis/components/ImageUpload'
import { MainLayout } from './layouts/MainLayout'

const DEMO_PARCEL_ID = '3b241101-e2bb-4255-8caf-4136c566a962'


export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<MapView />} />
        <Route 
          path="/diagnosis" 
          element={<ImageUpload parcelId={DEMO_PARCEL_ID} />} />
      </Route>
    </Routes>
  )
}
