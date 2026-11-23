package ug.caa.adpautomate.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
  Optional<Account> findByUsernameIgnoreCase(String username);

  boolean existsByUsernameIgnoreCase(String username);

  long countByActiveTrueAndRole(AccountRole role);

  List<Account> findAllByOrderByActiveDescDisplayNameAsc();

  List<Account> findByActiveTrueOrderByDisplayNameAsc();
}
