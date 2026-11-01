package app.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /auth/register}.
 *
 * @param email a valid email address that becomes the account's login
 * @param password the account password, at least 8 characters
 */
@Schema(name = "RegisterRequest", description = "Credentials used to create a new account.")
public record RegisterRequest(
    @Schema(example = "ana@example.com") @NotBlank @Email String email,
    @Schema(example = "at-least-8-characters")
        @NotBlank
        @Size(min = RegisterRequest.MINIMUM_PASSWORD_LENGTH)
        String password) {

  /** Minimum number of characters a password must have, as stated in the contract. */
  public static final int MINIMUM_PASSWORD_LENGTH = 8;
}
