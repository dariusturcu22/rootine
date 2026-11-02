package app.parcel.domain;

import app.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.locationtech.jts.geom.Polygon;

/**
 * A parcel, mapped to the {@code parcels} table. Its geometry is a JTS {@link Polygon} in WGS 84
 * (SRID {@link SpatialReference#WGS84_SRID}), stored by Hibernate Spatial in the PostGIS geometry
 * column. Never exposed by a controller; the {@code ParcelMapper} turns it into a {@code
 * ParcelResponse}.
 */
@Entity
@Table(name = "parcels")
public class Parcel {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false, updatable = false)
  private UserAccount owner;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 100)
  private String crop;

  @Column(nullable = false)
  private Polygon geometry;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Parcel() {
    // Required by JPA.
  }

  public Parcel(
      UUID id, UserAccount owner, String name, String crop, Polygon geometry, Instant createdAt) {
    this.id = id;
    this.owner = owner;
    this.name = name;
    this.crop = crop;
    this.geometry = geometry;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UserAccount getOwner() {
    return owner;
  }

  public void setOwner(UserAccount owner) {
    this.owner = owner;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCrop() {
    return crop;
  }

  public void setCrop(String crop) {
    this.crop = crop;
  }

  public Polygon getGeometry() {
    return geometry;
  }

  public void setGeometry(Polygon geometry) {
    this.geometry = geometry;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
