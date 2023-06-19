package pl.zajonz.librarytest.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import pl.zajonz.librarytest.model.Book;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Book> findWithLockingById(int id);

    Book findBookByAuthor(String author);

}
