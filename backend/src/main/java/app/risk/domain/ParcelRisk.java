package app.risk.domain;

import app.parcel.domain.Parcel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The latest weather risk result for one parcel, mapped to the {@code parcel_risk} table. There is
 * at most one row per parcel; a refresh replaces it. The primary key is the parcel id (shared with
 * {@link Parcel} through {@link MapsId}). The per-disease breakdown is stored as JSON.
 */
@Entity
@Table(name = "parcel_risk")
public class ParcelRisk {

  @Id
  @Column(name = "parcel_id", nullable = false, updatable = false)
  private UUID parcelId;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "parcel_id", nullable = false)
  private Parcel parcel;

  @Column(nullable = false, length = 20)
  private String level;

  @Column(name = "risk_index", nullable = false)
  private int riskIndex;

  @Column(nullable = false)
  private String reason;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private List<StoredDiseaseRisk> diseases;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  protected ParcelRisk() {
    // Required by JPA.
  }

  public ParcelRisk(
      Parcel parcel,
      String level,
      int riskIndex,
      String reason,
      List<StoredDiseaseRisk> diseases,
      Instant generatedAt) {
    this.parcel = parcel;
    this.level = level;
    this.riskIndex = riskIndex;
    this.reason = reason;
    this.diseases = diseases;
    this.generatedAt = generatedAt;
  }

  public UUID getParcelId() {
    return parcelId;
  }

  public Parcel getParcel() {
    return parcel;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public int getRiskIndex() {
    return riskIndex;
  }

  public void setRiskIndex(int riskIndex) {
    this.riskIndex = riskIndex;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public List<StoredDiseaseRisk> getDiseases() {
    return diseases;
  }

  public void setDiseases(List<StoredDiseaseRisk> diseases) {
    this.diseases = diseases;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(Instant generatedAt) {
    this.generatedAt = generatedAt;
  }
}
