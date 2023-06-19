package pl.zajonz.librarytest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.zajonz.librarytest.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

}
