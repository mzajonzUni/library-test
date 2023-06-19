package pl.zajonz.librarytest.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.event.model.BookEvent;
import pl.zajonz.librarytest.event.model.InfoEvent;
import pl.zajonz.librarytest.model.Book;
import pl.zajonz.librarytest.model.Category;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.repository.BookRepository;
import pl.zajonz.librarytest.repository.CategoryRepository;
import pl.zajonz.librarytest.repository.UserRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public Book create(Book book, int categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(
                () -> new EntityNotFoundException("Not found category with id: " + categoryId));

        book.setCategory(category);
        book.setBlocked(false);
        book.setState(State.READY);

        Book saved = bookRepository.save(book);

        publisher.publishEvent(new InfoEvent(book + " has been created"));
        publisher.publishEvent(new BookEvent(book));

        return saved;
    }

    @Override
    @Transactional
    public Book blockBook(int id) {
        Book book = bookRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found book with id: " + id));
        book.setBlocked(true);

        publisher.publishEvent(new InfoEvent(book + " has been blocked"));

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

        Book save = bookRepository.save(book);

        publisher.publishEvent(new InfoEvent(book + " has been borrowed"));

        return save;
    }

    @Override
    @Transactional
    public Book returnBook(String name, String role, int id) {
        Book book = bookRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Not found book with id: " + id));

        if (book.getUser() == null) {
            throw new IllegalArgumentException("Book with id: " + id + " is not borrowed");
        }

        if (role.equals("[ROLE_CUSTOMER]") && !book.getUser().getUsername().equals(name)) {
            throw new IllegalArgumentException("No access to book with id: " + id);
        }

        book.setState(State.READY);
        book.setUser(null);
        book.setFromDate(null);
        book.setToDate(null);

        Book saved = bookRepository.save(book);

        publisher.publishEvent(new InfoEvent(book + " was returned by user: " + name + " with role " + role));

        return saved;
    }

    @Override
    public Page<Book> getAll(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }
}
