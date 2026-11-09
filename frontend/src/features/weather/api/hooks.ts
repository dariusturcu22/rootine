import { useQueries, useQuery } from '@tanstack/react-query'
import { getParcelRisk, getParcels } from '../../../lib/apiClient'

export function useRiskParcels() {
  return useQuery({
    queryKey: ['parcels'],
    queryFn: getParcels,
  })
}

export function useParcelRisks(parcelIds: string[]) {
  return useQueries({
    queries: parcelIds.map((id) => ({
      queryKey: ['parcels', id, 'risk'],
      queryFn: () => getParcelRisk(id),
      retry: false,
    })),
  })
}
