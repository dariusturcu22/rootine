package app.parcel.repository;

import app.parcel.domain.Parcel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Parcel}. A user only ever sees their own parcels. */
public interface ParcelRepository extends JpaRepository<Parcel, UUID> {

  /** Lists one user's parcels, newest first, as the parcel list endpoint returns them. */
  List<Parcel> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

  /** Finds a parcel by id but only if it belongs to the given owner, so 404 hides others' ids. */
  Optional<Parcel> findByIdAndOwnerId(UUID id, UUID ownerId);
}
