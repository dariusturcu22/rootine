package app.diagnosis.mapper;

import app.diagnosis.domain.Diagnosis;
import app.diagnosis.dto.DiagnosisResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps the {@link Diagnosis} entity to the {@link DiagnosisResponse} DTO, so controllers never
 * expose the entity. The model's result is stored nested in {@link DiagnosisResult}; this mapper
 * flattens it into the flat response the contract defines.
 */
@Mapper(componentModel = "spring")
public interface DiagnosisMapper {

  @Mapping(source = "parcel.id", target = "parcelId")
  @Mapping(source = "result.status", target = "status")
  @Mapping(source = "result.crop", target = "crop")
  @Mapping(source = "result.cropCheck", target = "cropCheck")
  @Mapping(source = "result.topPredictions", target = "topPredictions")
  @Mapping(source = "result.supportLevel", target = "supportLevel")
  DiagnosisResponse toResponse(Diagnosis diagnosis);
}
