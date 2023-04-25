package pl.zajonz.librarytest.book;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.zajonz.librarytest.book.model.Book;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.user.UserRepository;
import pl.zajonz.librarytest.user.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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
    public Book borrowBook(String name, int id, LocalDate to) {
        if (to.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Date 'to' cannot be before today's date");
        }
        Book book = bookRepository.findById(id).orElseThrow(
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
    public Book returnBook(String name, int id) {
        User user = userRepository.findByUsername(name).orElseThrow(
                () -> new UsernameNotFoundException("Not found user with username: " + name));
        Book book = bookRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found book with id: " + id));
        if (user.getRole().equals("ROLE_USER") && !user.getBooks().contains(book)) {
            throw new IllegalArgumentException("No access to book with id: " + id);
        }

        book.setState(State.READY);
        book.setUser(null);
        book.setFromDate(null);
        book.setToDate(null);

        return bookRepository.save(book);
    }

    @Override
    public List<Book> getAll(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return bookRepository.findAll(pageable)
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public List<Book> getAllByUser(String name, String auth, int userId) {
        if (auth.equals("[ROLE_EMPLOYEE]")) {
            return bookRepository.findAllByUser_Id(userId);
        }
        return bookRepository.findAllByUser_Username(name);
    }
}
