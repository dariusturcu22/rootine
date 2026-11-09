import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createParcel,
  deleteParcel,
  getCrops,
  getParcels,
  updateParcel,
  type ParcelInput,
} from '../../../lib/apiClient'

export const parcelKeys = {
  all: ['parcels'] as const,
  crops: ['crops'] as const,
}

export function useParcelQueryClient() {
  return useQueryClient()
}

export function useParcels() {
  return useQuery({
    queryKey: parcelKeys.all,
    queryFn: getParcels,
  })
}

export function useCrops() {
  return useQuery({
    queryKey: parcelKeys.crops,
    queryFn: getCrops,
  })
}

export function useCreateParcel(onSuccess: () => void | Promise<void>) {
  return useMutation({ mutationFn: createParcel, onSuccess })
}

export function useUpdateParcel(
  onSuccess: () => void | Promise<void>,
  onError: () => void | Promise<void>,
) {
  return useMutation({
    mutationFn: ({ id, parcel }: { id: string; parcel: ParcelInput }) => updateParcel(id, parcel),
    onSuccess,
    onError,
  })
}

export function useDeleteParcel(onSuccess: () => void | Promise<void>) {
  return useMutation({ mutationFn: deleteParcel, onSuccess })
}
