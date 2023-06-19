package pl.zajonz.librarytest.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pl.zajonz.librarytest.model.Book;
import pl.zajonz.librarytest.model.User;

import java.util.List;

public interface UserService {

    User create(User user);

    Page<User> getAll(Pageable pageable);

    List<Book> getAllBooks(String name, String role, int id);

}
