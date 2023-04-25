package pl.zajonz.librarytest.book;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.zajonz.librarytest.book.model.Book;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Integer> {

    List<Book> findAllByUser_Id(int id);

    List<Book> findAllByUser_Username(String username);
}
