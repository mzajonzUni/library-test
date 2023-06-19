package pl.zajonz.librarytest.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class BookServiceImplTest {

    @InjectMocks
    private BookServiceImpl bookService;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void testCreate_ResultsInBookBeingReturned() {
        //given
        Book book = Book.builder()
                .title("Test")
                .author("Test")
                .build();
        Category category = Category.builder()
                .id(1)
                .name("Test")
                .build();

        when(categoryRepository.findById(anyInt())).thenReturn(Optional.of(category));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        //when
        Book returned = bookService.create(book, 1);

        //then
        assertEquals(book, returned);
        assertFalse(returned.isBlocked());
        assertEquals(State.READY, returned.getState());
        verify(eventPublisher, times(1)).publishEvent(any(BookEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testCreate_ThrowsEntityNotFoundException() {
        //given
        String exceptionMsg = "Not found category with id: 1";
        Book book = Book.builder()
                .title("Test")
                .author("Test")
                .build();

        when(categoryRepository.findById(anyInt())).thenReturn(Optional.empty());
        //when //then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> bookService.create(book, 1));
        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(BookEvent.class));
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testBlockBook_CorrectValues_ResultsInBookBeingReturned() {
        //given
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .build();
        when(bookRepository.findById(anyInt())).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        //when
        Book returned = bookService.blockBook(1);
        //then
        assertEquals(book, returned);
        assertTrue(returned.isBlocked());

        verify(eventPublisher, times(1)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testBlockBook_IncorrectBookId_ResultsInEntityNotFoundException() {
        //given
        String exceptionMsg = "Not found book with id: 1";

        when(bookRepository.findById(anyInt())).thenReturn(Optional.empty());

        //when //then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> bookService.blockBook(1));
        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testBorrowBook_CorrectValues_ResultsInBookBeingReturned() {
        //given
        User user = User.builder()
                .id(1)
                .username("Test")
                .role("ROLE_USER")
                .build();
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .state(State.READY)
                .build();
        Book book2 = Book.builder()
                .id(2)
                .title("Test")
                .author("Test")
                .user(user)
                .toDate(LocalDate.now().plusDays(10))
                .state(State.BORROWED)
                .build();
        user.setBooks(Set.of(book));
        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.of(user));
        when(bookRepository.findWithLockingById(anyInt())).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book2);
        //when
        Book returned = bookService.borrowBook("Test", 1, LocalDate.now().plusDays(10));
        //then
        assertEquals(book2, returned);
        verify(eventPublisher, times(1)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testBorrowBook_IncorrectToDate_ResultsInIllegalArgumentException() {
        //given
        String exceptionMsg = "Date 'to' cannot be before today's date";
        //when //then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.borrowBook("Test", 1, LocalDate.now().minusDays(1)));

        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testBorrowBook_BookNotFound_ResultsInEntityNotFoundException() {
        //given
        String exceptionMsg = "Not found book with id: 1";

        when(bookRepository.findWithLockingById(anyInt())).thenReturn(Optional.empty());
        //when //then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> bookService.borrowBook("Test", 1, LocalDate.now().plusDays(10)));

        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testBorrowBook_BookBlocked_ResultsInEntityNotFoundException() {
        //given
        String exceptionMsg = "Book with id: 1 cannot be borrowed";
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .isBlocked(true)
                .state(State.READY)
                .build();

        when(bookRepository.findWithLockingById(anyInt())).thenReturn(Optional.of(book));
        //when //then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.borrowBook("Test", 1, LocalDate.now().plusDays(10)));

        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testBorrowBook_BookBorrowed_ResultsInIllegalArgumentException() {
        //given
        String exceptionMsg = "Book is borrowed to: " + LocalDate.now().plusDays(10);
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .state(State.BORROWED)
                .toDate(LocalDate.now().plusDays(10))
                .build();

        when(bookRepository.findWithLockingById(anyInt())).thenReturn(Optional.of(book));
        //when //then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.borrowBook("Test", 1, LocalDate.now().plusDays(10)));

        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testBorrowBook_UserNotFound_ResultsInUsernameNotFoundException() {
        //given
        String exceptionMsg = "Not found user with username: Test";
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .state(State.READY)
                .toDate(LocalDate.now().plusDays(10))
                .build();

        when(bookRepository.findWithLockingById(anyInt())).thenReturn(Optional.of(book));
        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.empty());
        //when //then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> bookService.borrowBook("Test", 1, LocalDate.now().plusDays(10)));

        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testReturnBook_CorrectValues_ResultsInBookBeingReturned() {
        //given
        User user = User.builder()
                .id(1)
                .username("Test")
                .role("ROLE_USER")
                .build();
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .user(user)
                .state(State.READY)
                .build();
        user.setBooks(Set.of(book));

        when(bookRepository.findById(anyInt())).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(book);
        //when
        Book returned = bookService.returnBook(user.getUsername(), user.getRole(), 1);

        //then
        assertEquals(book, returned);
        verify(eventPublisher, times(1)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testReturnBook_Employee_ResultsInBookBeingReturned() {
        //given
        User user = User.builder()
                .id(1)
                .username("Test")
                .role("ROLE_USER")
                .build();
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .user(user)
                .state(State.READY)
                .build();
        user.setBooks(Set.of(book));

        when(bookRepository.findById(anyInt())).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(book);
        //when
        Book returned = bookService.returnBook("Employee", "[ROLE_EMPLOYEE]", 1);

        //then
        assertEquals(book, returned);
        verify(eventPublisher, times(1)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testReturnBook_BookNotFound_ResultsInEntityNotFoundException() {
        //given
        String exceptionMsg = "Not found book with id: 1";

        when(bookRepository.findById(anyInt())).thenReturn(Optional.empty());
        //when //then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> bookService.returnBook("Test", "[ROLE_USER]", 1));

        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testReturnBook_BookNotBorrowed_ResultsInIllegalArgumentException() {
        //given
        String exceptionMsg = "Book with id: 1 is not borrowed";
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .state(State.READY)
                .build();

        when(bookRepository.findById(anyInt())).thenReturn(Optional.of(book));

        //when //then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.returnBook("TestNoAccess", "[ROLE_USER]", 1));

        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testReturnBook_NoAccess_ResultsInIllegalArgumentException() {
        //given
        String exceptionMsg = "No access to book with id: 1";
        User user = User.builder()
                .id(1)
                .username("Test")
                .role("ROLE_CUSTOMER")
                .books(Set.of())
                .build();
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .state(State.BORROWED)
                .user(user)
                .build();

        when(bookRepository.findById(anyInt())).thenReturn(Optional.of(book));

        //when //then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.returnBook("TestNoAccess", "[ROLE_CUSTOMER]", 1));

        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testGetAll_ResultsInListBookBeingReturned() {
        //given
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .build();
        Page<Book> bookPage = new PageImpl<>(List.of(book));

        when(bookRepository.findAll(any(Pageable.class))).thenReturn(bookPage);
        //when
        Page<Book> returned = bookService.getAll(PageRequest.of(1, 2));
        //then
        assertEquals(bookPage, returned);
    }

}