package app.risk.repository;

import app.risk.domain.ParcelRisk;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for the latest {@link ParcelRisk} stored per parcel. */
public interface ParcelRiskRepository extends JpaRepository<ParcelRisk, UUID> {

  /** Latest stored risk for every parcel owned by one user, for the heatmap (BE-03). */
  List<ParcelRisk> findByParcelOwnerId(UUID ownerId);
}
