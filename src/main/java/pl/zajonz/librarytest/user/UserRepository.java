package pl.zajonz.librarytest.user;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.zajonz.librarytest.user.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
}
