package app.diagnosis.domain;

import app.parcel.domain.Parcel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A diagnosis, mapped to the {@code diagnoses} table. It keeps only the path to the stored photo,
 * never the image. The model's result is stored as JSON in the {@code result} column; the model
 * version is a separate column. Never exposed by a controller; the {@code DiagnosisMapper} turns it
 * into a {@code DiagnosisResponse}.
 */
@Entity
@Table(name = "diagnoses")
public class Diagnosis {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "parcel_id", nullable = false, updatable = false)
  private Parcel parcel;

  @Column(name = "photo_path", nullable = false, length = 512)
  private String photoPath;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private DiagnosisResult result;

  @Column(name = "model_version", nullable = false, length = 100)
  private String modelVersion;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Diagnosis() {
    // Required by JPA.
  }

  public Diagnosis(
      UUID id,
      Parcel parcel,
      String photoPath,
      DiagnosisResult result,
      String modelVersion,
      Instant createdAt) {
    this.id = id;
    this.parcel = parcel;
    this.photoPath = photoPath;
    this.result = result;
    this.modelVersion = modelVersion;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Parcel getParcel() {
    return parcel;
  }

  public void setParcel(Parcel parcel) {
    this.parcel = parcel;
  }

  public String getPhotoPath() {
    return photoPath;
  }

  public void setPhotoPath(String photoPath) {
    this.photoPath = photoPath;
  }

  public DiagnosisResult getResult() {
    return result;
  }

  public void setResult(DiagnosisResult result) {
    this.result = result;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
