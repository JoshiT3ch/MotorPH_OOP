package motorph.repository;

import motorph.model.UserAccount;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<UserAccount> findAll();
    Optional<UserAccount> findByUsername(String username);
    void saveAll(List<UserAccount> accounts);
}
