package app.user.web;

import app.common.error.ProblemDetailResponse;
import app.user.dto.LoginRequest;
import app.user.dto.LoginResponse;
import app.user.dto.RegisterRequest;
import app.user.dto.UserResponse;
import app.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registration and login. These are the only endpoints that do not need a token. */
@RestController
@RequestMapping(path = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auth", description = "Registration and login.")
public class AuthController {

  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(
      summary = "Register a new account",
      description = "Creates an account. It does not log the user in, so login comes next.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "The account was created.",
        content = @Content(schema = @Schema(implementation = UserResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "An account with this email already exists.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "The email or the password is invalid.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class)))
  })
  @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse register(@Valid @RequestBody RegisterRequest registerRequest) {
    return authService.register(registerRequest);
  }

  @Operation(
      summary = "Log in and obtain an access token",
      description = "Returns a JWT valid for one hour. There is no refresh token in Demo 1.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Login succeeded.",
        content = @Content(schema = @Schema(implementation = LoginResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Wrong email or wrong password.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class))),
    @ApiResponse(
        responseCode = "422",
        description = "The body is invalid.",
        content =
            @Content(
                mediaType = PROBLEM_JSON,
                schema = @Schema(implementation = ProblemDetailResponse.class)))
  })
  @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
    return authService.login(loginRequest);
  }
}
