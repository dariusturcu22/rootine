package app.user.service;

import app.user.domain.UserAccount;
import app.user.dto.LoginResponse;
import app.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates the local demo account if needed and issues its one-hour access token. */
@Service
@ConditionalOnProperty(name = "app.demo-token.enabled", havingValue = "true")
public class DemoTokenService {

  public static final UUID DEMO_USER_ID = UUID.fromString("c8de92db-8442-4c48-b781-1398081fd680");
  public static final String DEMO_EMAIL = "demo@rootine.local";
  private static final long TOKEN_LIFETIME_SECONDS = 3600;

  private final UserRepository userRepository;
  private final JwtEncoder jwtEncoder;

  public DemoTokenService(UserRepository userRepository, JwtEncoder jwtEncoder) {
    this.userRepository = userRepository;
    this.jwtEncoder = jwtEncoder;
  }

  @Transactional
  public LoginResponse issueToken() {
    UserAccount user = getOrCreateDemoUser();
    Instant issuedAt = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.getId().toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(TOKEN_LIFETIME_SECONDS))
            .build();
    String token =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new LoginResponse(token, "Bearer", TOKEN_LIFETIME_SECONDS);
  }

  private UserAccount getOrCreateDemoUser() {
    return userRepository
        .findById(DEMO_USER_ID)
        .orElseGet(
            () ->
                userRepository.save(
                    new UserAccount(
                        DEMO_USER_ID,
                        DEMO_EMAIL,
                        new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()),
                        Instant.now())));
  }
}
