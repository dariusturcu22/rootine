package app.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.user.domain.UserAccount;
import app.user.dto.LoginRequest;
import app.user.dto.LoginResponse;
import app.user.dto.RegisterRequest;
import app.user.dto.UserResponse;
import app.user.mapper.UserMapper;
import app.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** Unit tests for the auth service: hashing, email normalization, and credential checks. */
class AuthServiceTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final JwtEncoder jwtEncoder = mock(JwtEncoder.class);
  private final UserMapper userMapper =
      userAccount -> new UserResponse(userAccount.getId(), userAccount.getEmail());

  private final AuthService authService =
      new AuthService(userRepository, userMapper, passwordEncoder, jwtEncoder);

  @Test
  void registerNormalizesEmailAndHashesPassword() {
    when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(any(UserAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response =
        authService.register(new RegisterRequest("  Ana@Example.com  ", "a-strong-password"));

    ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
    verify(userRepository).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getEmail()).isEqualTo("ana@example.com");
    assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("a-strong-password");
    assertThat(passwordEncoder.matches("a-strong-password", saved.getValue().getPasswordHash()))
        .isTrue();
    assertThat(response.email()).isEqualTo("ana@example.com");
  }

  @Test
  void registerRejectsDuplicateEmail() {
    when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

    assertThatThrownBy(
            () -> authService.register(new RegisterRequest("ana@example.com", "a-strong-password")))
        .isInstanceOf(EmailAlreadyRegisteredException.class);
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void loginIssuesTokenForCorrectPassword() {
    UserAccount account =
        new UserAccount(
            UUID.randomUUID(),
            "ana@example.com",
            passwordEncoder.encode("a-strong-password"),
            Instant.now());
    when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(account));
    Jwt jwt =
        Jwt.withTokenValue("signed.jwt.value")
            .header("alg", "HS256")
            .subject(account.getId().toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

    LoginResponse response =
        authService.login(new LoginRequest("Ana@example.com", "a-strong-password"));

    assertThat(response.accessToken()).isEqualTo("signed.jwt.value");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresIn()).isEqualTo(3600);
  }

  @Test
  void loginRejectsWrongPasswordWithTheSameErrorAsUnknownEmail() {
    UserAccount account =
        new UserAccount(
            UUID.randomUUID(),
            "ana@example.com",
            passwordEncoder.encode("a-strong-password"),
            Instant.now());
    when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(account));
    when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> authService.login(new LoginRequest("ana@example.com", "wrong-password")))
        .isInstanceOf(InvalidCredentialsException.class);
    assertThatThrownBy(
            () -> authService.login(new LoginRequest("unknown@example.com", "a-strong-password")))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void getCurrentUserReturnsTheAccountForTheTokenSubject() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id))
        .thenReturn(Optional.of(new UserAccount(id, "ana@example.com", "hash", Instant.now())));

    UserResponse response = authService.getCurrentUser(id);

    assertThat(response.id()).isEqualTo(id);
    assertThat(response.email()).isEqualTo("ana@example.com");
  }

  @Test
  void getCurrentUserFailsWhenTheAccountIsGone() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.getCurrentUser(id))
        .isInstanceOf(UserNotFoundException.class);
  }
}
