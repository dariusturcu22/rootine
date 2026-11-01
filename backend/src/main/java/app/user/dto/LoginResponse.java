package app.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Successful response of {@code POST /auth/login}.
 *
 * @param accessToken the signed JWT to send as {@code Authorization: Bearer <token>}
 * @param tokenType the token type; always {@code Bearer}
 * @param expiresIn the token's lifetime in seconds
 */
@Schema(name = "LoginResponse", description = "A newly issued access token.")
public record LoginResponse(
    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
    @Schema(example = "Bearer") String tokenType,
    @Schema(example = "3600") long expiresIn) {}
