package pl.zajonz.librarytest.book;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pl.zajonz.librarytest.book.model.Book;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.user.UserRepository;
import pl.zajonz.librarytest.user.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @InjectMocks
    private BookServiceImpl bookService;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserRepository userRepository;

    @Test
    void testCreate_ResultsInBookBeingReturned() {
        //given
        Book book = Book.builder()
                .title("Test")
                .author("Test")
                .build();
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        //when
        Book returned = bookService.create(book);
        //then
        assertEquals(book, returned);
        assertFalse(returned.isBlocked());
        assertEquals(State.READY, returned.getState());
    }

    @Test
    void testBlockBook_CorrectValues_ResultsInBookBeingReturned() {
        //given
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .build();
        when(bookRepository.findById(any(Integer.class))).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        //when
        Book returned = bookService.blockBook(1);
        //then
        assertEquals(book, returned);
        assertTrue(returned.isBlocked());
    }

    @Test
    void testBlockBook_IncorrectBookId_ResultsInEntityNotFoundException() {
        //given
        String exceptionMsg = "Not found book with id: 1";

        when(bookRepository.findById(any(Integer.class))).thenReturn(Optional.empty());

        //when //then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> bookService.blockBook(1));
        assertEquals(exceptionMsg, exception.getMessage());
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
                .id(1)
                .title("Test")
                .author("Test")
                .user(user)
                .toDate(LocalDate.now().plusDays(10))
                .state(State.BORROWED)
                .build();
        user.setBooks(Set.of(book));
        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.of(user));
        when(bookRepository.findById(any(Integer.class))).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book2);
        //when
        Book returned = bookService.borrowBook("Test", 1, LocalDate.now().plusDays(10));
        //then
        assertEquals(book2, returned);
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
    }

    @Test
    void testBorrowBook_IncorrectUser_ResultsInUsernameNotFoundException() {
        //given
        String exceptionMsg = "Not found user with username: Test";

        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.empty());
        //when //then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> bookService.borrowBook("Test", 1, LocalDate.now().plusDays(10)));

        assertEquals(exceptionMsg, exception.getMessage());
    }

    @Test
    void testBorrowBook_IncorrectBook_ResultsInEntityNotFoundException() {
        //given
        String exceptionMsg = "Not found book with id: 1";
        User user = User.builder()
                .id(1)
                .username("Test")
                .role("ROLE_USER")
                .build();
        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.of(user));
        when(bookRepository.findById(any(Integer.class))).thenReturn(Optional.empty());
        //when //then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> bookService.borrowBook("Test", 1, LocalDate.now().plusDays(10)));

        assertEquals(exceptionMsg, exception.getMessage());
    }

    @Test
    void testBorrowBook_BookBlocked_ResultsInEntityNotFoundException() {
        //given
        String exceptionMsg = "Book with id: 1 cannot be borrowed";
        User user = User.builder()
                .id(1)
                .username("Test")
                .role("ROLE_USER")
                .build();
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .isBlocked(true)
                .state(State.READY)
                .build();

        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.of(user));
        when(bookRepository.findById(any(Integer.class))).thenReturn(Optional.of(book));
        //when //then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.borrowBook("Test", 1, LocalDate.now().plusDays(10)));

        assertEquals(exceptionMsg, exception.getMessage());
    }

    @Test
    void testBorrowBook_StateBorrowed_ResultsInIllegalArgumentException() {
        //given
        String exceptionMsg = "Book is borrowed to: " + LocalDate.now().plusDays(10);
        User user = User.builder()
                .id(1)
                .username("Test")
                .role("ROLE_USER")
                .build();
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .state(State.BORROWED)
                .toDate(LocalDate.now().plusDays(10))
                .build();

        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.of(user));
        when(bookRepository.findById(any(Integer.class))).thenReturn(Optional.of(book));
        //when //then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.borrowBook("Test", 1, LocalDate.now().plusDays(10)));

        assertEquals(exceptionMsg, exception.getMessage());
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
                .state(State.READY)
                .build();
        user.setBooks(Set.of(book));
        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.of(user));
        when(bookRepository.findById(any(Integer.class))).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(book);
        //when
        Book returned = bookService.returnBook(user.getUsername(), 1);

        //then
        assertEquals(book, returned);
    }

    @Test
    void testReturnBook_IncorrectBook_ResultsInEntityNotFoundException() {
        //given
        User user = User.builder()
                .id(1)
                .username("Test")
                .role("ROLE_USER")
                .build();
        String exceptionMsg = "Not found book with id: 1";

        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.of(user));
        when(bookRepository.findById(any(Integer.class))).thenReturn(Optional.empty());
        //when //then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> bookService.returnBook("Test", 1));

        assertEquals(exceptionMsg, exception.getMessage());
    }

    @Test
    void testReturnBook_IncorrectUser_ResultsInUsernameNotFoundException() {
        //given
        String exceptionMsg = "Not found user with username: Test";

        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.empty());
        //when //then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> bookService.returnBook("Test", 1));

        assertEquals(exceptionMsg, exception.getMessage());
    }

    @Test
    void testReturnBook_NoAccess_ResultsInIllegalArgumentException() {
        //given
        String exceptionMsg = "No access to book with id: 1";

        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .state(State.READY)
                .build();
        User user = User.builder()
                .id(1)
                .username("Test")
                .role("ROLE_USER")
                .books(Set.of())
                .build();
        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.of(user));
        when(bookRepository.findById(any(Integer.class))).thenReturn(Optional.of(book));

        //when //then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.returnBook("Test", 1));

        assertEquals(exceptionMsg, exception.getMessage());
    }

    @Test
    void testGetAll_ResultsInListBookBeingReturned() {
        //given
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .build();
        List<Book> bookList = List.of(book);
        Page<Book> bookPage = new PageImpl<>(bookList);

        when(bookRepository.findAll(any(Pageable.class))).thenReturn(bookPage);
        //when
        List<Book> returned = bookService.getAll(0, 1);
        //then
        assertEquals(bookList, returned);
    }

    @Test
    void testGetAllByUser_RoleUser_ResultsInListBookBeingReturned() {
        //given
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .build();
        User user = User.builder()
                .id(1)
                .username("Test")
                .role("ROLE_USER")
                .books(Set.of(book))
                .build();
        when(bookRepository.findAllByUser_Username(any(String.class))).thenReturn(List.of(book));
        //when
        List<Book> returned = bookService.getAllByUser("Test", user.getAuthorities().toString(), 0);

        //then
        assertEquals(user.getBooks().stream().toList(), returned);
    }

    @Test
    void testGetAllByUser_RoleEmployee_ResultsInListBookBeingReturned() {
        //given
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Test")
                .build();
        List<Book> bookList = List.of(book);
        User employee = User.builder()
                .id(1)
                .username("TestE")
                .role("ROLE_EMPLOYEE")
                .build();
        when(bookRepository.findAllByUser_Id(any(Integer.class))).thenReturn(bookList);
        //when
        List<Book> returned = bookService.getAllByUser("TestE", employee.getAuthorities().toString(), 2);

        //then
        assertEquals(bookList, returned);
    }
}