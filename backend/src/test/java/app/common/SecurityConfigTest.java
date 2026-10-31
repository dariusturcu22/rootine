package app.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class SecurityConfigTest {

  private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes";

  private final SecurityConfig config = new SecurityConfig();

  @Test
  void decoderAcceptsTokenSignedWithTheSameSecret() throws Exception {
    JwtDecoder decoder = config.jwtDecoder(SECRET);

    Jwt jwt = decoder.decode(signedToken(SECRET));

    assertThat(jwt.getSubject()).isEqualTo("farmer-1");
  }

  @Test
  void decoderRejectsTokenSignedWithAnotherSecret() throws Exception {
    JwtDecoder decoder = config.jwtDecoder(SECRET);

    assertThatThrownBy(() -> decoder.decode(signedToken("another-secret-that-is-also-32-bytes!")))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void shortSecretIsRejectedAtStartup() {
    assertThatThrownBy(() -> config.jwtDecoder("short")).isInstanceOf(IllegalStateException.class);
  }

  private static String signedToken(String secret) throws Exception {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject("farmer-1")
            .expirationTime(new Date(System.currentTimeMillis() + 60_000))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(new MACSigner(secret.getBytes()));
    return jwt.serialize();
  }
}
