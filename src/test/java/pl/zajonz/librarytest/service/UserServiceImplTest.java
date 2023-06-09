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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import pl.zajonz.librarytest.event.model.InfoEvent;
import pl.zajonz.librarytest.model.Book;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher eventPublisher;


    @Test
    void testCreate_CorrectValues_ResultsInUserBeingReturned() {
        //given
        User user = User.builder()
                .username("Test")
                .password("####")
                .build();

        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("####");
        when(userRepository.save(any(User.class))).thenReturn(user);
        //when
        User returned = userService.create(user);

        //then
        assertEquals(user, returned);
        verify(eventPublisher, times(1)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testCreate_InCorrectUser_ResultsInIllegalArgumentException() {
        //given
        String exceptionMsg = "User with username: Test already exists";
        User user = User.builder()
                .username("Test")
                .password("Test")
                .build();
        User userToCreate = User.builder()
                .username("Test")
                .password("Test")
                .build();

        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.of(user));
        //when //then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.create(userToCreate));

        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testCreate_InCorrectEmail_ResultsInIllegalArgumentException() {
        //given
        User user = User.builder()
                .username("Test")
                .password("Test")
                .email("test@test.pl")
                .build();
        User userToCreate = User.builder()
                .username("Test1")
                .password("Test")
                .email("test@test.pl")
                .build();
        String exceptionMsg = "User with email: " + userToCreate.getEmail() + " already exists";

        when(userRepository.findByEmail(userToCreate.getEmail())).thenReturn(Optional.of(user));
        //when //then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.create(userToCreate));

        assertEquals(exceptionMsg, exception.getMessage());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testGetAll_CorrectValues_ResultsInListUserBeingReturned() {
        //given
        User user = User.builder()
                .username("Test")
                .password("Test")
                .build();
        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);
        //when
        Page<User> returned = userService.getAll(PageRequest.of(1, 2));

        //then
        assertEquals(userPage, returned);
    }

    @Test
    void testGetAllBooks_User_CorrectValues() {
        //given
        Book book = Book.builder().build();
        User user = User.builder()
                .username("Test")
                .password("Test")
                .books(Set.of(book))
                .role("ROLE_CUSTOMER")
                .build();
        List<Book> bookList = List.of(book);

        when(userRepository.findById(anyInt())).thenReturn(Optional.of(user));
        //when
        List<Book> returned = userService.getAllBooks("Test", "ROLE_CUSTOMER", 1);

        //then
        assertEquals(bookList, returned);
    }

    @Test
    void testGetAllBooks_Employee_CorrectValues() {
        //given
        Book book = Book.builder().build();
        User user = User.builder()
                .username("Test")
                .password("Test")
                .books(Set.of(book))
                .role("ROLE_CUSTOMER")
                .build();
        List<Book> bookList = List.of(book);

        when(userRepository.findById(anyInt())).thenReturn(Optional.of(user));
        //when
        List<Book> returned = userService.getAllBooks("Test", "ROLE_EMPLOYEE", 1);

        //then
        assertEquals(bookList, returned);
    }

    @Test
    void testGetAllBooks_ResultsInEntityNotFoundException() {
        //given
        String exceptionMsg = "Not found user with id: 1";

        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
        //when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.getAllBooks("Test", "ROLE_CUSTOMER", 1));
        //then
        assertEquals(exceptionMsg, exception.getMessage());
    }

    @Test
    void testGetAllBooks_ResultsInIllegalArgumentException() {
        //given
        String exceptionMsg = "No access to book with id: 1";
        Book book = Book.builder().build();
        User user = User.builder()
                .username("Test")
                .password("Test")
                .books(Set.of(book))
                .role("ROLE_CUSTOMER")
                .build();

        when(userRepository.findById(anyInt())).thenReturn(Optional.of(user));
        //when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.getAllBooks("TU", "ROLE_CUSTOMER", 1));
        //then
        assertEquals(exceptionMsg, exception.getMessage());
    }

    @Test
    void testLoadUserByUsername_CorrectValues_ResultsInUserDetailsBeingReturned() {
        //given
        User user = User.builder()
                .username("Test")
                .password("Test")
                .build();
        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.of(user));
        //when
        UserDetails returned = userService.loadUserByUsername(user.getUsername());

        //then
        assertEquals(user, returned);
    }

    @Test
    void testLoadUserByUsername_IncorrectUser_ResultsInUsernameNotFoundException() {
        //given
        String exceptionMsg = "User not found with username: Test";
        User user = User.builder()
                .username("Test")
                .password("Test")
                .build();
        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.empty());
        //when //then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(user.getUsername()));

        assertEquals(exceptionMsg, exception.getMessage());
    }
}