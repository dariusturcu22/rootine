package app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * A user as returned by {@code POST /auth/register} and {@code GET /me}.
 *
 * @param id the user's UUID, set by the backend
 * @param email the user's email address
 */
@Schema(name = "UserResponse", description = "A user account.")
public record UserResponse(
    @Schema(example = "7c9e6679-7425-40de-944b-e07fc1f90ae7") UUID id,
    @Schema(example = "ana@example.com") String email) {}
