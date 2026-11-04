package app.user.web;

import app.user.dto.LoginResponse;
import app.user.service.DemoTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Local-demo token endpoint; it is absent unless explicitly enabled by configuration. */
@RestController
@RequestMapping(path = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auth", description = "Registration, login, and local demo access.")
@ConditionalOnProperty(name = "app.demo-token.enabled", havingValue = "true")
public class DemoTokenController {

  private final DemoTokenService demoTokenService;

  public DemoTokenController(DemoTokenService demoTokenService) {
    this.demoTokenService = demoTokenService;
  }

  @Operation(
      summary = "Get a local demo access token",
      description =
          "Creates the local demo user if needed and returns a JWT valid for one hour. "
              + "Enable only for a local demo.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The demo access token.",
        content = @Content(schema = @Schema(implementation = LoginResponse.class)))
  })
  @PostMapping("/demo-token")
  public LoginResponse getDemoToken() {
    return demoTokenService.issueToken();
  }
}
