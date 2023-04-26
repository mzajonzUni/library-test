package pl.zajonz.librarytest.book;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.zajonz.librarytest.book.model.Book;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.user.UserRepository;
import pl.zajonz.librarytest.user.model.User;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Override
    public Book create(Book book) {
        book.setBlocked(false);
        book.setState(State.READY);
        return bookRepository.save(book);
    }

    @Override
    public Book blockBook(int id) {
        Book book = bookRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found book with id: " + id));
        book.setBlocked(true);
        return bookRepository.save(book);
    }

    @Override
    @Transactional
    public Book borrowBook(String name, int id, LocalDate to) {
        if (to.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Date 'to' cannot be before today's date");
        }
        Book book = bookRepository.findWithLockingById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found book with id: " + id));

        if (book.isBlocked()) {
            throw new IllegalArgumentException("Book with id: " + id + " cannot be borrowed");
        }

        if (book.getState().equals(State.BORROWED)) {
            throw new IllegalArgumentException("Book is borrowed to: " + book.getToDate());
        }
        User user = userRepository.findByUsername(name).orElseThrow(
                () -> new UsernameNotFoundException("Not found user with username: " + name));

        book.setState(State.BORROWED);
        book.setUser(user);
        book.setFromDate(LocalDate.now());
        book.setToDate(to);

        return bookRepository.save(book);
    }

    @Override
    public Book returnBook(String name, String role, int id) {
        Book book = bookRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found book with id: " + id));

        if (book.getUser() == null) {
            throw new IllegalArgumentException("Book with id: " + id + " is not borrowed");
        }

        if (role.equals("[ROLE_USER]") && !book.getUser().getUsername().equals(name)) {
            throw new IllegalArgumentException("No access to book with id: " + id);
        }

        book.setState(State.READY);
        book.setUser(null);
        book.setFromDate(null);
        book.setToDate(null);

        return bookRepository.save(book);
    }

    @Override
    public Page<Book> getAll(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        return bookRepository.findAll(pageable);
    }
}
