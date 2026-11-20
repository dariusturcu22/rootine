package app.user.service;

import app.user.domain.UserAccount;
import app.user.dto.LoginRequest;
import app.user.dto.LoginResponse;
import app.user.dto.RegisterRequest;
import app.user.dto.UserResponse;
import app.user.mapper.UserMapper;
import app.user.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Email registration, login, and current-user lookup backed by persisted accounts. */
@Service
public class AuthService {

  /** Access-token lifetime in seconds. One hour, as the API contract documents. */
  static final long TOKEN_LIFETIME_SECONDS = 3600;

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtEncoder jwtEncoder;

  public AuthService(
      UserRepository userRepository,
      UserMapper userMapper,
      PasswordEncoder passwordEncoder,
      JwtEncoder jwtEncoder) {
    this.userRepository = userRepository;
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.jwtEncoder = jwtEncoder;
  }

  /**
   * Creates an account. Emails are normalized so the same address cannot register twice under a
   * different casing. The unique index on {@code users.email} is the source of truth, so two
   * concurrent registrations of the same email cannot both succeed: the second one fails the insert
   * and is reported as a conflict.
   */
  @Transactional
  public UserResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyRegisteredException();
    }
    UserAccount account =
        new UserAccount(
            UUID.randomUUID(), email, passwordEncoder.encode(request.password()), Instant.now());
    try {
      return userMapper.toResponse(userRepository.saveAndFlush(account));
    } catch (DataIntegrityViolationException exception) {
      // A concurrent registration inserted the same email between the check and the flush.
      throw new EmailAlreadyRegisteredException();
    }
  }

  /**
   * Verifies the credentials and issues a one-hour JWT. An unknown email and a wrong password give
   * the same {@link InvalidCredentialsException}, so a caller cannot tell which emails exist.
   */
  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {
    UserAccount account =
        userRepository
            .findByEmail(normalizeEmail(request.email()))
            .orElseThrow(InvalidCredentialsException::new);
    if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }
    return issueToken(account);
  }

  /** Returns the account the token's subject points at. */
  @Transactional(readOnly = true)
  public UserResponse getCurrentUser(UUID userId) {
    return userRepository
        .findById(userId)
        .map(userMapper::toResponse)
        .orElseThrow(UserNotFoundException::new);
  }

  private LoginResponse issueToken(UserAccount account) {
    Instant issuedAt = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(account.getId().toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(TOKEN_LIFETIME_SECONDS))
            .build();
    String token =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new LoginResponse(token, "Bearer", TOKEN_LIFETIME_SECONDS);
  }

  private static String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }
}
