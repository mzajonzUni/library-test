package pl.zajonz.librarytest.user;

import org.springframework.data.domain.Page;
import pl.zajonz.librarytest.book.model.Book;
import pl.zajonz.librarytest.user.model.User;

import java.util.List;

public interface UserService {

    User create(User user);

    Page<User> getAll(int pageNo, int pageSize);

    List<Book> getAllBooks(String name, String role, int id);
}
