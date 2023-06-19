package pl.zajonz.librarytest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.model.*;
import pl.zajonz.librarytest.model.command.CreateBookCommand;
import pl.zajonz.librarytest.repository.BookRepository;
import pl.zajonz.librarytest.repository.CategoryRepository;
import pl.zajonz.librarytest.repository.UserRepository;
import pl.zajonz.librarytest.service.MessageSender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @SpyBean
    private MessageSender messageSender;
    @MockBean
    private RabbitTemplate rabbitTemplate;

    @AfterEach
    public void clearDatabase() {
        userRepository.deleteAll();
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testCreate_RoleEmployee_CorrectValues() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        User user = User.builder()
                .id(1)
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test.pl")
                .role("ROLE_TEST")
                .password("TEST")
                .subscribedCategories(Set.of(category))
                .build();
        userRepository.save(user);

        CreateBookCommand command = CreateBookCommand.builder()
                .title("Titletest")
                .author("Authortest")
                .categoryId(category.getId())
                .build();

        //when //then
        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo(command.getTitle())))
                .andExpect(jsonPath("$.author", equalTo(command.getAuthor())))
                .andExpect(jsonPath("$.blocked", equalTo(false)))
                .andExpect(jsonPath("$.state", equalTo(State.READY.toString())))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        int bookId = JsonPath.parse(responseString).read("$.id", Integer.class);

        Book createdBook = bookRepository.findById(bookId).orElse(null);
        assertNotNull(createdBook);
        assertEquals(command.getAuthor(), createdBook.getAuthor());
        assertEquals(command.getTitle(), createdBook.getTitle());
        assertNull(createdBook.getUser());
        assertNull(createdBook.getToDate());
        assertNull(createdBook.getFromDate());
        assertFalse(createdBook.isBlocked());
        assertEquals(State.READY, createdBook.getState());

        verify(messageSender, times(1)).sendEmailInfo(any(Book.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(InfoMessage.class));
        verify(messageSender, times(1)).sendInfo(anyString());
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "EMPLOYEE")
    void testCreate_RoleEmployee_ShouldReturnEntityNotFoundException() throws Exception {
        //given
        CreateBookCommand command = CreateBookCommand.builder()
                .title("Titletest")
                .author("Authortest")
                .categoryId(100)
                .build();

        //when //then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found category with id: 100")));

        verify(messageSender, times(0)).sendEmailInfo(any(Book.class));
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(InfoMessage.class));
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "EMPLOYEE")
    void testCreate_RoleEmployee_Validation() throws Exception {
        //given
        CreateBookCommand command = CreateBookCommand.builder()
                .title("")
                .author("")
                .build();

        //when //then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Validation errors")))
                .andExpect(jsonPath("$.violations[*].message",
                        containsInAnyOrder("title cannot be blank",
                                "author cannot be blank",
                                "title has to match the pattern",
                                "author has to match the pattern",
                                "must be greater than or equal to 1")));

        verify(messageSender, times(0)).sendEmailInfo(any(Book.class));
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(InfoMessage.class));
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testBlockBook_RoleEmployee_CorrectValues() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(false)
                .state(State.BORROWED)
                .fromDate(LocalDate.now())
                .toDate(LocalDate.now().plusDays(10))
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(patch("/api/v1/books/" + book.getId() + "/block"))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo(book.getTitle())))
                .andExpect(jsonPath("$.author", equalTo(book.getAuthor())))
                .andExpect(jsonPath("$.blocked", equalTo(book.isBlocked())));

        Book savedBook = bookRepository.findById(book.getId()).orElse(null);
        assertNotNull(savedBook);
        assertEquals(book.getAuthor(), savedBook.getAuthor());
        assertEquals(book.getTitle(), savedBook.getTitle());
        assertTrue(savedBook.isBlocked());

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(1)).sendInfo(anyString());
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testBlockBook_RoleCustomer_ShouldReturnForbidden() throws Exception {
        //given

        //when //then
        mockMvc.perform(patch("/api/v1/books/1/block"))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testBlockBook_RoleEmployee_BookNotFound_ShouldReturnEntityNotFoundException() throws Exception {
        //given

        //when //then
        mockMvc.perform(patch("/api/v1/books/100/block"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found book with id: 100")));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testBorrowBook_RoleCustomer_CorrectValues() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);
        User user = User.builder()
                .id(1)
                .username("user")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test.pl")
                .role("ROLE_TEST")
                .password("user")
                .locked(false)
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(false)
                .state(State.READY)
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(put("/api/v1/books/" + book.getId() + "/borrow")
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo(book.getTitle())))
                .andExpect(jsonPath("$.author", equalTo(book.getAuthor())))
                .andExpect(jsonPath("$.username", equalTo(user.getUsername())))
                .andExpect(jsonPath("$.state", equalTo(State.BORROWED.toString())));

        Book savedBook = bookRepository.findById(book.getId()).orElse(null);
        assertNotNull(savedBook);
        assertEquals(LocalDate.now().plusDays(10), savedBook.getToDate());
        assertEquals(user.getUsername(), savedBook.getUser().getUsername());
        assertNotNull(savedBook.getFromDate());
        assertEquals(State.BORROWED, savedBook.getState());

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(1)).sendInfo(anyString());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testBorrowBook_RoleEmployee_ShouldReturnForbidden() throws Exception {
        //given

        //when //then
        mockMvc.perform(put("/api/v1/books/1/borrow")
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testBorrowBook_RoleCustomer_IncorrectToDate_ShouldThrowIllegalArgumentException() throws Exception {
        //given

        //when //then
        mockMvc.perform(put("/api/v1/books/1/borrow")
                        .param("to", LocalDate.now().minusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Date 'to' cannot be before today's date")));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testBorrowBook_RoleCustomer_BookNotFound_ShouldThrowEntityNotFoundException() throws Exception {
        //given

        //when //then
        mockMvc.perform(put("/api/v1/books/100/borrow")
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found book with id: 100")));
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testBorrowBook_RoleCustomer_BookBlocked_ShouldThrowIllegalArgumentException() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(true)
                .state(State.READY)
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(put("/api/v1/books/" + book.getId() + "/borrow")
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Book with id: "
                        + book.getId() + " cannot be borrowed")));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testBorrowBook_RoleCustomer_BookBorrowed_ShouldThrowIllegalArgumentException() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);
        User user = User.builder()
                .id(1)
                .username("user")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test.pl")
                .role("ROLE_TEST")
                .password("user")
                .locked(false)
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(false)
                .state(State.BORROWED)
                .fromDate(LocalDate.now())
                .toDate(LocalDate.now().plusDays(10))
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(put("/api/v1/books/" + book.getId() + "/borrow")
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Book is borrowed to: "
                        + book.getToDate())));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testReturnBook_RoleCustomer_CorrectValues() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);
        User user = User.builder()
                .username("user")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test.pl")
                .role("ROLE_TEST")
                .password("user")
                .locked(false)
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(false)
                .user(user)
                .state(State.BORROWED)
                .fromDate(LocalDate.now())
                .toDate(LocalDate.now().plusDays(10))
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(patch("/api/v1/books/" + book.getId() + "/return"))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo(book.getTitle())))
                .andExpect(jsonPath("$.author", equalTo(book.getAuthor())))
                .andExpect(jsonPath("$.state", equalTo(State.READY.toString())));

        Book returnedBook = bookRepository.findById(book.getId()).orElse(null);
        assertNotNull(returnedBook);
        assertEquals(book.getAuthor(), returnedBook.getAuthor());
        assertEquals(book.getTitle(), returnedBook.getTitle());
        assertNull(returnedBook.getUser());
        assertNull(returnedBook.getToDate());
        assertNull(returnedBook.getFromDate());
        assertEquals(State.READY, returnedBook.getState());

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(1)).sendInfo(anyString());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testReturnBook_RoleEmployee_CorrectValues() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);
        User user = User.builder()
                .username("user")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test.pl")
                .role("ROLE_TEST")
                .password("user")
                .locked(false)
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(false)
                .user(user)
                .state(State.BORROWED)
                .fromDate(LocalDate.now())
                .toDate(LocalDate.now().plusDays(10))
                .build();
        bookRepository.save(book);
        //when //then
        mockMvc.perform(patch("/api/v1/books/" + book.getId() + "/return"))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo(book.getTitle())))
                .andExpect(jsonPath("$.author", equalTo(book.getAuthor())))
                .andExpect(jsonPath("$.state", equalTo(State.READY.toString())));

        Book returnedBook = bookRepository.findById(book.getId()).orElse(null);
        assertNotNull(returnedBook);
        assertEquals(book.getAuthor(), returnedBook.getAuthor());
        assertEquals(book.getTitle(), returnedBook.getTitle());
        assertNull(returnedBook.getUser());
        assertNull(returnedBook.getToDate());
        assertNull(returnedBook.getFromDate());
        assertEquals(State.READY, returnedBook.getState());

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(1)).sendInfo(anyString());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testReturnBook_RoleCustomer_BookNotFound_ShouldReturnEntityNotFoundException() throws Exception {
        //given

        //when //then
        mockMvc.perform(patch("/api/v1/books/1/return"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found book with id: 1")));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testReturnBook_RoleCustomer_AccessDenied_ShouldReturnIllegalArgumentException() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);
        User user = User.builder()
                .username("user")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test.pl")
                .role("ROLE_TEST")
                .password("user")
                .locked(false)
                .build();
        userRepository.save(user);
        User user2 = User.builder()
                .username("TEst")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test1.pl")
                .role("ROLE_TEST")
                .password("user")
                .locked(false)
                .build();
        userRepository.save(user2);
        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(true)
                .user(user2)
                .state(State.BORROWED)
                .fromDate(LocalDate.now())
                .toDate(LocalDate.now().plusDays(10))
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(patch("/api/v1/books/" + book.getId() + "/return"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("No access to book with id: "
                        + book.getId())));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testReturnBook_RoleCustomer_BookNotBorrowed_ShouldReturnIllegalArgumentException() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);
        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(true)
                .state(State.BORROWED)
                .fromDate(LocalDate.now())
                .toDate(LocalDate.now().plusDays(10))
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(patch("/api/v1/books/" + book.getId() + "/return"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Book with id: " +
                        book.getId() + " is not borrowed")));

        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    void testGetAll_AllUsers_CorrectValues() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);
        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(true)
                .state(State.BORROWED)
                .fromDate(LocalDate.now())
                .toDate(LocalDate.now().plusDays(10))
                .build();
        bookRepository.save(book);

        Book book2 = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(false)
                .state(State.READY)
                .fromDate(LocalDate.now())
                .toDate(LocalDate.now().plusDays(10))
                .build();
        bookRepository.save(book2);

        Book book3 = Book.builder()
                .title("Test")
                .author("Testowy")
                .state(State.READY)
                .category(category)
                .fromDate(LocalDate.now())
                .toDate(LocalDate.now().plusDays(10))
                .build();
        bookRepository.save(book3);


        //when //then
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "0")
                        .param("size", "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", equalTo(book.getTitle())))
                .andExpect(jsonPath("$.content[0].author", equalTo(book.getAuthor())))
                .andExpect(jsonPath("$.content[1].title", equalTo(book2.getTitle())))
                .andExpect(jsonPath("$.content[1].author", equalTo(book2.getAuthor())))
                .andExpect(jsonPath("$.numberOfElements", equalTo(2)))
                .andExpect(jsonPath("$.size", equalTo(2)))
                .andExpect(jsonPath("$.totalPages", equalTo(2)));

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

}