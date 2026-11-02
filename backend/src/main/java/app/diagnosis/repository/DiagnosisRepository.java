package app.diagnosis.repository;

import app.diagnosis.domain.Diagnosis;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Diagnosis}. A user only ever sees their own diagnoses. */
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

  /** Lists the diagnoses owned by one user (through the parcel's owner), newest first. */
  List<Diagnosis> findByParcelOwnerIdOrderByCreatedAtDesc(UUID ownerId);

  /** Lists the diagnoses of one parcel owned by one user, newest first. */
  List<Diagnosis> findByParcelIdAndParcelOwnerIdOrderByCreatedAtDesc(UUID parcelId, UUID ownerId);

  /** Finds a diagnosis by id but only if its parcel belongs to the given owner. */
  Optional<Diagnosis> findByIdAndParcelOwnerId(UUID id, UUID ownerId);
}
