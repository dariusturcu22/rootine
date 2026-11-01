package app.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /auth/login}. Uses the same fields as registration.
 *
 * @param email the account's email address
 * @param password the account password
 */
@Schema(name = "LoginRequest", description = "Credentials used to obtain an access token.")
public record LoginRequest(
    @Schema(example = "ana@example.com") @NotBlank @Email String email,
    @Schema(example = "at-least-8-characters") @NotBlank String password) {}
