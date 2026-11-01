package app.user.repository;

import app.user.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link UserAccount}. */
public interface UserRepository extends JpaRepository<UserAccount, UUID> {

  /** Finds an account by its email address, used by login and the registration conflict check. */
  Optional<UserAccount> findByEmail(String email);

  /** Whether an account with this email already exists, used to return 409 on registration. */
  boolean existsByEmail(String email);
}
