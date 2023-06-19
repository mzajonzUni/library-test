package pl.zajonz.librarytest.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.output.WaitingConsumer;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.model.Book;
import pl.zajonz.librarytest.model.Category;
import pl.zajonz.librarytest.model.InfoMessage;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.model.command.CreateBookCommand;
import pl.zajonz.librarytest.repository.BookRepository;
import pl.zajonz.librarytest.repository.CategoryRepository;
import pl.zajonz.librarytest.repository.UserRepository;
import pl.zajonz.librarytest.service.MessageSender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

public class IntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private Integer port;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @SpyBean
    private MessageSender messageSender;
    @Captor
    private ArgumentCaptor<Long> longArgumentCaptor;
    @Captor
    private ArgumentCaptor<LocalDateTime> localDateTimeArgumentCaptor;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + port;
        userRepository.deleteAll();
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void test() {
        assertTrue(mySQLContainer.isRunning());
        assertTrue(rabbitMQContainer.isRunning());
        assertTrue(loggingContainer.isRunning());
        assertTrue(emailSenderContainer.isRunning());
    }

    @Test
    void testCreate() throws TimeoutException {
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        User user = User.builder()
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("zajonz.mateusz@gmail.com")
                .role("ROLE_EMPLOYEE")
                .password(passwordEncoder.encode("test"))
                .subscribedCategories(Set.of(category))
                .build();
        userRepository.save(user);

        CreateBookCommand command = CreateBookCommand.builder()
                .title("Titletest")
                .author("Authortest")
                .categoryId(category.getId())
                .build();

        given()
                .auth()
                .basic(user.getUsername(), "test")
                .contentType(ContentType.JSON)
                .body(command)
                .when()
                .post("/api/v1/books")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .assertThat()
                .body("author", equalTo(command.getAuthor()))
                .body("title", equalTo(command.getTitle()))
                .body("state", equalTo(State.READY.toString()))
                .body("user", equalTo(null));

        Book book = bookRepository.findBookByAuthor(command.getAuthor());

        assertNotNull(book);
        assertEquals(book.getAuthor(), command.getAuthor());
        assertEquals(book.getTitle(), command.getTitle());

        InfoMessage infoMessage = new InfoMessage();
        infoMessage.setBookId(book.getId());
        infoMessage.setBook_author(command.getAuthor());
        infoMessage.setBook_title(command.getTitle());
        infoMessage.setBook_category(category.getName());
        infoMessage.setEmail(user.getEmail());
        infoMessage.setUser_firstName(user.getFirstname());
        infoMessage.setUser_lastName(user.getLastname());

        verify(messageSender).sendPerformanceInfo(any(User.class), longArgumentCaptor.capture(),
                anyString(), localDateTimeArgumentCaptor.capture());

        long executionTime = longArgumentCaptor.getValue();
        LocalDateTime methodStart = localDateTimeArgumentCaptor.getValue();

        String performanceInfo = "User with id: " + user.getId() + ", email: "
                + user.getEmail() + ", started method: BookController.create(..) at time: "
                + methodStart + " and the method lasted: " + executionTime + " ms";

        WaitingConsumer consumer = new WaitingConsumer();

        loggingContainer.followOutput(consumer, STDOUT);

        consumer.waitUntil(frame ->
                        frame.getUtf8String().contains("Email has been sent with parameters " + infoMessage),
                5, TimeUnit.SECONDS);

        String logs = loggingContainer.getLogs(STDOUT);
        assertTrue(logs.contains(performanceInfo));
        assertTrue(logs.contains(book + " has been created"));
    }

    @Test
    void testCreate_Forbidden() {
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        User user = User.builder()
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("zajonz.mateusz@gmail.com")
                .role("ROLE_CUSTOMER")
                .password(passwordEncoder.encode("test"))
                .subscribedCategories(Set.of(category))
                .build();
        userRepository.save(user);

        CreateBookCommand command = CreateBookCommand.builder()
                .title("Titletest")
                .author("Authortest")
                .categoryId(category.getId())
                .build();

        given()
                .auth()
                .basic(user.getUsername(), "test")
                .contentType(ContentType.JSON)
                .body(command)
                .when()
                .post("/api/v1/books")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        Book book = bookRepository.findBookByAuthor(command.getAuthor());
        assertNull(book);

        verify(messageSender, times(0)).sendEmailInfo(any(Book.class));
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class), any(Long.class),
                anyString(), any(LocalDateTime.class));
    }

    @Test
    void testBorrowBook() {
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        User user = User.builder()
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("zajonz.mateusz@gmail.com")
                .role("ROLE_CUSTOMER")
                .password(passwordEncoder.encode("test"))
                .subscribedCategories(Set.of(category))
                .build();
        userRepository.save(user);

        Book book = Book.builder()
                .author("test")
                .title("Test")
                .category(category)
                .state(State.READY)
                .isBlocked(false)
                .build();
        bookRepository.save(book);

        LocalDate borrowTo = LocalDate.now().plusDays(10);

        given()
                .auth()
                .basic(user.getUsername(), "test")
                .when()
                .put("/api/v1/books/" + book.getId() + "/borrow?to=" + borrowTo)
                .then()
                .statusCode(HttpStatus.ACCEPTED.value())
                .assertThat()
                .body("id", equalTo(book.getId()))
                .body("author", equalTo(book.getAuthor()))
                .body("title", equalTo(book.getTitle()))
                .body("state", equalTo(State.BORROWED.toString()))
                .body("toDate", equalTo(borrowTo.toString()));

        verify(messageSender).sendPerformanceInfo(any(User.class), longArgumentCaptor.capture(),
                anyString(), localDateTimeArgumentCaptor.capture());

        Book borrowedBook = bookRepository.findById(book.getId()).orElse(null);
        assertNotNull(borrowedBook);
        assertEquals(borrowTo, borrowedBook.getToDate());
        assertNotNull(borrowedBook.getUser());

        long executionTime = longArgumentCaptor.getValue();
        LocalDateTime methodStart = localDateTimeArgumentCaptor.getValue();

        String performanceInfo = "User with id: " + user.getId() + ", email: "
                + user.getEmail() + ", started method: BookController.borrowBook(..) at time: "
                + methodStart + " and the method lasted: " + executionTime + " ms";

        String logs = loggingContainer.getLogs(STDOUT);
        assertTrue(logs.contains(performanceInfo));
        assertTrue(logs.contains(borrowedBook + " has been borrowed"));
    }

    @Test
    void testBlockBook() {
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        User user = User.builder()
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("zajonz.mateusz@gmail.com")
                .role("ROLE_EMPLOYEE")
                .password(passwordEncoder.encode("test"))
                .subscribedCategories(Set.of(category))
                .build();
        userRepository.save(user);

        Book book = Book.builder()
                .author("test")
                .title("Test")
                .category(category)
                .state(State.READY)
                .isBlocked(false)
                .build();
        bookRepository.save(book);

        given()
                .auth()
                .basic(user.getUsername(), "test")
                .when()
                .patch("/api/v1/books/" + book.getId() + "/block")
                .then()
                .statusCode(HttpStatus.ACCEPTED.value())
                .assertThat()
                .body("id", equalTo(book.getId()))
                .body("author", equalTo(book.getAuthor()))
                .body("title", equalTo(book.getTitle()))
                .body("state", equalTo(State.READY.toString()));

        verify(messageSender).sendPerformanceInfo(any(User.class), longArgumentCaptor.capture(),
                anyString(), localDateTimeArgumentCaptor.capture());

        Book blockedBook = bookRepository.findById(book.getId()).orElse(null);
        assertNotNull(blockedBook);
        assertTrue(blockedBook.isBlocked());

        long executionTime = longArgumentCaptor.getValue();
        LocalDateTime methodStart = localDateTimeArgumentCaptor.getValue();

        String performanceInfo = "User with id: " + user.getId() + ", email: "
                + user.getEmail() + ", started method: BookController.blockBook(..) at time: "
                + methodStart + " and the method lasted: " + executionTime + " ms";

        String logs = loggingContainer.getLogs(STDOUT);
        assertTrue(logs.contains(performanceInfo));
        assertTrue(logs.contains(blockedBook + " has been blocked"));
    }

    @Test
    void testBlockBook_Forbidden() {
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        User user = User.builder()
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("zajonz.mateusz@gmail.com")
                .role("ROLE_CUSTOMER")
                .password(passwordEncoder.encode("test"))
                .subscribedCategories(Set.of(category))
                .build();
        userRepository.save(user);

        Book book = Book.builder()
                .author("test")
                .title("Test")
                .category(category)
                .state(State.READY)
                .isBlocked(false)
                .build();
        bookRepository.save(book);

        given()
                .auth()
                .basic(user.getUsername(), "test")
                .when()
                .patch("/api/v1/books/" + book.getId() + "/block")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        verify(messageSender, times(0)).sendInfo(anyString());
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class), any(Long.class),
                anyString(), any(LocalDateTime.class));

    }

    @Test
    void testSubscribe() throws TimeoutException {
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        User user = User.builder()
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("zajonz.mateusz@gmail.com")
                .role("ROLE_CUSTOMER")
                .password(passwordEncoder.encode("test"))
                .subscribedCategories(new HashSet<>())
                .build();
        userRepository.save(user);

        given()
                .auth()
                .basic(user.getUsername(), "test")
                .when()
                .patch("/api/v1/categories/" + category.getId() + "/subscribe")
                .then()
                .statusCode(HttpStatus.ACCEPTED.value());

        verify(messageSender).sendPerformanceInfo(any(User.class), longArgumentCaptor.capture(),
                anyString(), localDateTimeArgumentCaptor.capture());

        long executionTime = longArgumentCaptor.getValue();
        LocalDateTime methodStart = localDateTimeArgumentCaptor.getValue();

        String performanceInfo = "User with id: " + user.getId() + ", email: "
                + user.getEmail() + ", started method: CategoryController.subscribe(..) at time: "
                + methodStart + " and the method lasted: " + executionTime + " ms";

        WaitingConsumer consumer = new WaitingConsumer();
        loggingContainer.followOutput(consumer, STDOUT);

        consumer.waitUntil(frame ->
                        frame.getUtf8String().contains(performanceInfo),
                5, TimeUnit.SECONDS);

        String logs = loggingContainer.getLogs();
        assertTrue(logs.contains(user + " has subscribed category: " + category));
    }

    @Test
    void testSubscribe_Forbidden() {
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        User user = User.builder()
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("zajonz.mateusz@gmail.com")
                .role("ROLE_EMPLOYEE")
                .password(passwordEncoder.encode("test"))
                .subscribedCategories(new HashSet<>())
                .build();
        userRepository.save(user);

        given()
                .auth()
                .basic(user.getUsername(), "test")
                .when()
                .patch("/api/v1/categories/" + category.getId() + "/subscribe")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        verify(messageSender, times(0)).sendInfo(anyString());
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class), any(Long.class),
                anyString(), any(LocalDateTime.class));
    }

}
