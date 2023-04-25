package pl.zajonz.librarytest.book;

import pl.zajonz.librarytest.book.model.Book;

import java.time.LocalDate;
import java.util.List;

public interface BookService {
    Book create(Book book);

    Book blockBook(int id);

    Book borrowBook(String name, int id, LocalDate to);

    Book returnBook(String name, int id);

    List<Book> getAll(int pageNo, int pageSize);

    List<Book> getAllByUser(String name, String auth, int userId);
}
