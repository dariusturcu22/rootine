package app.diagnosis.domain;

import app.common.SupportLevel;
import app.diagnosis.dto.CropCheck;
import app.diagnosis.dto.DiagnosisStatus;
import app.diagnosis.dto.Prediction;
import java.util.List;

/**
 * The model's result for one diagnosis, stored as a JSON document in the {@code diagnoses.result}
 * column. These are exactly the fields the AI service returns; the model version is kept in its own
 * column on the entity, not here.
 *
 * @param status the outcome of the diagnosis
 * @param crop the crop used
 * @param cropCheck whether the photo's crop matched the parcel's crop, or {@code null} when no
 *     check was possible
 * @param topPredictions the predictions; empty for {@code invalid_image} or {@code
 *     crop_not_supported}
 * @param supportLevel how well the crop is supported
 */
public record DiagnosisResult(
    DiagnosisStatus status,
    String crop,
    CropCheck cropCheck,
    List<Prediction> topPredictions,
    SupportLevel supportLevel) {}
