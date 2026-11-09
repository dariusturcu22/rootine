import { useMutation } from '@tanstack/react-query'
import { createDiagnosis } from './diagnosisApi'

export function useCreateDiagnosis() {
  return useMutation({
    mutationFn: ({ image, parcelId }: { image: File; parcelId: string }) =>
      createDiagnosis(image, parcelId),
  })
}
