/**
 * This file is generated from docs/api/backend.md.
 * Do not make direct changes to the file.
 */

export type paths = {
  '/auth/register': {
    post: operations['register']
  }
  '/auth/login': {
    post: operations['login']
  }
  '/auth/demo-token': {
    post: operations['demoToken']
  }
  '/me': {
    get: operations['getMe']
  }
  '/crops': {
    get: operations['getCrops']
  }
  '/parcels': {
    get: operations['listParcels']
    post: operations['createParcel']
  }
  '/parcels/{id}': {
    get: operations['getParcel']
    put: operations['replaceParcel']
    delete: operations['deleteParcel']
  }
  '/diagnoses': {
    get: operations['listDiagnoses']
    post: operations['createDiagnosis']
  }
  '/diagnoses/{id}': {
    get: operations['getDiagnosis']
  }
  '/parcels/{id}/risk': {
    get: operations['getParcelRisk']
  }
  '/risk/heatmap': {
    get: operations['getHeatmap']
  }
  '/alerts/stream': {
    get: operations['getAlertStream']
  }
}

export type webhooks = Record<string, never>

export interface components {
  schemas: {
    ProblemDetail: {
      type: string
      title: string
      status: number
      detail: string
    }
    RegisterRequest: {
      email: string
      password: string
    }
    LoginRequest: components['schemas']['RegisterRequest']
    UserResponse: {
      id: string
      email: string
    }
    LoginResponse: {
      accessToken: string
      tokenType: 'Bearer'
      expiresIn: number
    }
    CropResponse: {
      crop: string
      diagnosisSupported: boolean
      weatherSupported: boolean
      supportLevel: 'lab_and_field_tested' | 'lab_only' | 'not_supported'
      weatherRules: string[]
      classes: string[]
    }
    PolygonGeometry: {
      type: 'Polygon'
      coordinates: [number, number][][]
    }
    ParcelRequest: {
      name: string
      crop: string
      geometry: components['schemas']['PolygonGeometry']
    }
    ParcelResponse: components['schemas']['ParcelRequest'] & {
      id: string
      createdAt: string
    }
    DiagnosisStatus: 'confident' | 'unsure' | 'invalid_image' | 'crop_not_supported'
    CropCheck: 'ok' | 'mismatch'
    Prediction: {
      label: string
      disease: string
      probability: number
    }
    DiagnosisResponse: {
      id: string
      parcelId: string
      status: components['schemas']['DiagnosisStatus']
      crop: string | null
      cropCheck: components['schemas']['CropCheck'] | null
      topPredictions: components['schemas']['Prediction'][]
      supportLevel: 'lab_and_field_tested' | 'lab_only' | 'not_supported'
      modelVersion: string
      createdAt: string
    }
    RiskLevel: 'low' | 'medium' | 'high' | 'not_modelled'
    DiseaseRisk: {
      disease: string
      model: string
      level: components['schemas']['RiskLevel']
      riskIndex: number
    }
    RiskResponse: {
      parcelId: string
      crop: string
      level: components['schemas']['RiskLevel']
      riskIndex: number
      reason: string
      diseases: components['schemas']['DiseaseRisk'][]
      generatedAt: string
    }
    HeatmapPoint: {
      parcelId: string
      name: string
      crop: string
      latitude: number
      longitude: number
      level: components['schemas']['RiskLevel']
      riskIndex: number
      generatedAt: string
    }
    AlertEvent: {
      parcelId: string
      parcelName: string
      crop: string
      level: 'high'
      riskIndex: number
      reason: string
      generatedAt: string
    }
  }
  responses: Record<string, never>
  parameters: Record<string, never>
  requestBodies: Record<string, never>
  headers: Record<string, never>
  pathItems: Record<string, never>
}

export type $defs = Record<string, never>

export interface operations {
  register: {
    requestBody: {
      content: { 'application/json': components['schemas']['RegisterRequest'] }
    }
    responses: {
      201: { content: { 'application/json': components['schemas']['UserResponse'] } }
    }
  }
  login: {
    requestBody: {
      content: { 'application/json': components['schemas']['LoginRequest'] }
    }
    responses: {
      200: { content: { 'application/json': components['schemas']['LoginResponse'] } }
    }
  }
  demoToken: operations['login']
  getMe: {
    responses: { 200: { content: { 'application/json': components['schemas']['UserResponse'] } } }
  }
  getCrops: {
    responses: {
      200: { content: { 'application/json': components['schemas']['CropResponse'][] } }
    }
  }
  listParcels: {
    responses: {
      200: { content: { 'application/json': components['schemas']['ParcelResponse'][] } }
    }
  }
  createParcel: {
    requestBody: {
      content: { 'application/json': components['schemas']['ParcelRequest'] }
    }
    responses: {
      201: { content: { 'application/json': components['schemas']['ParcelResponse'] } }
    }
  }
  getParcel: {
    responses: {
      200: { content: { 'application/json': components['schemas']['ParcelResponse'] } }
    }
  }
  replaceParcel: operations['createParcel']
  deleteParcel: {
    responses: { 204: { content: never } }
  }
  listDiagnoses: {
    responses: {
      200: { content: { 'application/json': components['schemas']['DiagnosisResponse'][] } }
    }
  }
  createDiagnosis: {
    requestBody: { content: { 'multipart/form-data': { image: Blob; parcelId: string } } }
    responses: {
      201: { content: { 'application/json': components['schemas']['DiagnosisResponse'] } }
    }
  }
  getDiagnosis: {
    responses: {
      200: { content: { 'application/json': components['schemas']['DiagnosisResponse'] } }
    }
  }
  getParcelRisk: {
    responses: {
      200: { content: { 'application/json': components['schemas']['RiskResponse'] } }
    }
  }
  getHeatmap: {
    responses: {
      200: { content: { 'application/json': components['schemas']['HeatmapPoint'][] } }
    }
  }
  getAlertStream: { responses: { 200: { content: { 'text/event-stream': string } } } }
}
