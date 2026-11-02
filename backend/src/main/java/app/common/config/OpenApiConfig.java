package app.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document metadata and the shared JWT bearer security scheme.
 *
 * <p>springdoc publishes the document at {@code /v3/api-docs}. The frontend generates its API types
 * from that document with {@code npm run gen:api}. The {@code bearerAuth} scheme declared here lets
 * every secured endpoint reference it, so the generated client knows an {@code Authorization:
 * Bearer <token>} header is required.
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
    info =
        @Info(
            title = "Rootine Backend API",
            version = "0.1.0",
            description =
                "REST API for the Rootine plant disease app. Errors use RFC 9457 Problem Details "
                    + "(application/problem+json). All times are ISO 8601 in UTC and IDs are UUID "
                    + "strings."))
@SecurityScheme(
    name = OpenApiConfig.BEARER_AUTH_SCHEME_NAME,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT issued by POST /auth/login. Send it as 'Authorization: Bearer <token>'.")
public class OpenApiConfig {

  /** Name of the JWT bearer security scheme referenced by secured endpoints. */
  public static final String BEARER_AUTH_SCHEME_NAME = "bearerAuth";
}
