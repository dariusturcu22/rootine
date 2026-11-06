package app.common.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless JWT security. Registration and login are public; the API documentation and the health
 * endpoint are public too. Every other endpoint needs a valid JWT.
 */
@Configuration
public class SecurityConfig {

  private static final int MIN_SECRET_BYTES = 32;

  private static final String REGISTER_PATH = "/auth/register";
  private static final String LOGIN_PATH = "/auth/login";
  private static final String DEMO_TOKEN_PATH = "/auth/demo-token";

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, @Value("${app.demo-token.enabled:false}") boolean demoTokenEnabled)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> {
              auth.requestMatchers(
                      "/actuator/health/**",
                      "/swagger-ui.html",
                      "/swagger-ui/**",
                      "/v3/api-docs/**")
                  .permitAll();
              auth.requestMatchers(HttpMethod.POST, REGISTER_PATH, LOGIN_PATH).permitAll();
              if (demoTokenEnabled) {
                auth.requestMatchers(HttpMethod.POST, DEMO_TOKEN_PATH).permitAll();
              }
              auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
              auth.anyRequest().authenticated();
            })
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
        .build();
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
    byte[] key = secret.getBytes(StandardCharsets.UTF_8);
    if (key.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT_SECRET must be set and at least " + MIN_SECRET_BYTES + " bytes long");
    }
    return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
  }

  @Bean
  JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secret) {
    byte[] key = secret.getBytes(StandardCharsets.UTF_8);
    if (key.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT_SECRET must be set and at least " + MIN_SECRET_BYTES + " bytes long");
    }
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }
}
