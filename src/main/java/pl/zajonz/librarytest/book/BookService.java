package pl.zajonz.librarytest.book;

import org.springframework.data.domain.Page;
import pl.zajonz.librarytest.book.model.Book;

import java.time.LocalDate;

public interface BookService {
    Book create(Book book);

    Book blockBook(int id);

    Book borrowBook(String name, int id, LocalDate to);

    Book returnBook(String name, String role, int id);

    Page<Book> getAll(int pageNo, int pageSize);

}
