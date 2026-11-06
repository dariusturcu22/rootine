package app.parcel.service;

import app.parcel.domain.Parcel;
import app.parcel.dto.ParcelRequest;
import app.parcel.dto.ParcelResponse;
import app.parcel.mapper.ParcelMapper;
import app.parcel.repository.ParcelRepository;
import app.user.domain.UserAccount;
import app.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages parcels belonging to the authenticated user. */
@Service
public class ParcelService {

  private final ParcelRepository parcelRepository;
  private final UserRepository userRepository;
  private final ParcelMapper parcelMapper;

  public ParcelService(
      ParcelRepository parcelRepository, UserRepository userRepository, ParcelMapper parcelMapper) {
    this.parcelRepository = parcelRepository;
    this.userRepository = userRepository;
    this.parcelMapper = parcelMapper;
  }

  @Transactional
  public ParcelResponse createParcel(UUID ownerId, ParcelRequest request) {
    UserAccount owner =
        userRepository.findById(ownerId).orElseThrow(ParcelOwnerNotFoundException::new);
    Polygon polygon = toValidPolygon(request.geometry());
    Parcel parcel =
        new Parcel(
            UUID.randomUUID(), owner, request.name(), request.crop(), polygon, Instant.now());
    return parcelMapper.toResponse(parcelRepository.save(parcel));
  }

  @Transactional(readOnly = true)
  public List<ParcelResponse> listParcels(UUID ownerId) {
    return parcelRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
        .map(parcelMapper::toResponse)
        .toList();
  }

  @Transactional
  public ParcelResponse replaceParcel(UUID ownerId, UUID parcelId, ParcelRequest request) {
    Parcel parcel =
        parcelRepository
            .findByIdAndOwnerId(parcelId, ownerId)
            .orElseThrow(ParcelNotFoundException::new);
    Polygon polygon = toValidPolygon(request.geometry());

    parcel.setName(request.name());
    parcel.setCrop(request.crop());
    parcel.setGeometry(polygon);
    return parcelMapper.toResponse(parcelRepository.save(parcel));
  }

  private Polygon toValidPolygon(app.parcel.dto.PolygonGeometry geometry) {
    if (geometry == null
        || !ParcelMapper.POLYGON_TYPE.equals(geometry.type())
        || geometry.coordinates() == null
        || geometry.coordinates().isEmpty()) {
      throw new InvalidParcelException("geometry must be a non-empty GeoJSON Polygon.");
    }

    for (List<List<Double>> ring : geometry.coordinates()) {
      validateRing(ring);
    }

    try {
      Polygon polygon = parcelMapper.toPolygon(geometry);
      if (polygon.isEmpty() || !polygon.isValid()) {
        throw new InvalidParcelException("geometry must be a valid polygon.");
      }
      return polygon;
    } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
      throw new InvalidParcelException("geometry must be a valid polygon.", exception);
    }
  }

  private static void validateRing(List<List<Double>> ring) {
    if (ring == null || ring.size() < 4) {
      throw new InvalidParcelException("Each polygon ring must have at least four positions.");
    }

    Coordinate first = toCoordinate(ring.get(0));
    Coordinate last = toCoordinate(ring.get(ring.size() - 1));
    if (!first.equals2D(last)) {
      throw new InvalidParcelException("Each polygon ring must be closed.");
    }

    for (List<Double> position : ring) {
      toCoordinate(position);
    }
  }

  private static Coordinate toCoordinate(List<Double> position) {
    if (position == null || position.size() != 2) {
      throw new InvalidParcelException(
          "Each polygon position must contain longitude and latitude.");
    }
    Double longitude = position.get(0);
    Double latitude = position.get(1);
    if (longitude == null
        || latitude == null
        || !Double.isFinite(longitude)
        || !Double.isFinite(latitude)
        || longitude < -180
        || longitude > 180
        || latitude < -90
        || latitude > 90) {
      throw new InvalidParcelException("Polygon coordinates must be valid WGS 84 positions.");
    }
    return new Coordinate(longitude, latitude);
  }
}
