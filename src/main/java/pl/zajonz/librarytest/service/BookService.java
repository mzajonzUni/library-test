package pl.zajonz.librarytest.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pl.zajonz.librarytest.model.Book;

import java.time.LocalDate;

public interface BookService {
    Book create(Book book, int categoryId);

    Book blockBook(int id);

    Book borrowBook(String name, int id, LocalDate to);

    Book returnBook(String name, String role, int id);

    Page<Book> getAll(Pageable pageable);

}
