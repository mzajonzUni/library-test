package pl.zajonz.librarytest.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.zajonz.librarytest.book.model.Book;
import pl.zajonz.librarytest.book.model.command.CreateBookCommand;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.user.UserRepository;
import pl.zajonz.librarytest.user.model.User;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void clearDatabase() {
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCreate_CorrectValues() throws Exception {
        //given
        CreateBookCommand command = CreateBookCommand.builder()
                .title("Test")
                .author("Test")
                .build();

        //when //then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Test")))
                .andExpect(jsonPath("$.author", equalTo("Test")))
                .andExpect(jsonPath("$.blocked", equalTo(false)))
                .andExpect(jsonPath("$.state", equalTo(State.READY.toString())));
    }

    @Test
    void testBlockBook_CorrectValues() throws Exception {
        //given
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(patch("/api/v1/books/block/" + book.getId()))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Test")))
                .andExpect(jsonPath("$.author", equalTo("Test")))
                .andExpect(jsonPath("$.blocked", equalTo(true)));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBlockBook_ShouldReturnForbidden() throws Exception {
        //given
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(patch("/api/v1/books/block/" + book.getId()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void testBlockBook_IncorrectValues() throws Exception {
        //given

        //when //then
        mockMvc.perform(patch("/api/v1/books/block/100"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found book with id: 100")));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBorrowBook() throws Exception {
        //given
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .state(State.READY)
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(put("/api/v1/books/borrow/" + book.getId())
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Test")))
                .andExpect(jsonPath("$.author", equalTo("Test")))
                .andExpect(jsonPath("$.userName", equalTo(user.getUsername())))
                .andExpect(jsonPath("$.state", equalTo(State.BORROWED.toString())));
    }

    @Test
    void testBorrowBook_ShouldReturnForbidden() throws Exception {
        //given
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .state(State.READY)
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(put("/api/v1/books/borrow/" + book.getId())
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBorrowBook_ShouldThrowIllegalArgumentException() throws Exception {
        //given
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .state(State.READY)
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(put("/api/v1/books/borrow/" + book.getId())
                        .param("to", LocalDate.now().minusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Date 'to' cannot be before today's date")));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBorrowBook_ShouldThrowEntityNotFoundException() throws Exception {
        //given
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);

        //when //then
        mockMvc.perform(put("/api/v1/books/borrow/100")
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found book with id: 100")));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBorrowBook_BookBlocked_ShouldThrowIllegalArgumentException() throws Exception {
        //given
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .state(State.READY)
                .isBlocked(true)
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(put("/api/v1/books/borrow/" + book.getId())
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Book with id: "
                        + book.getId() + " cannot be borrowed")));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBorrowBook_BookBorrowed_ShouldThrowIllegalArgumentException() throws Exception {
        //given
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .state(State.BORROWED)
                .toDate(LocalDate.now().plusDays(10))
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(put("/api/v1/books/borrow/" + book.getId())
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Book is borrowed to: "
                        + book.getToDate())));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testReturnBook() throws Exception {
        //given
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .user(user)
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(patch("/api/v1/books/return/" + book.getId()))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Test")))
                .andExpect(jsonPath("$.author", equalTo("Test")))
                .andExpect(jsonPath("$.state", equalTo(State.READY.toString())));
    }

    @Test
    void testReturnBook_EMPLOYEE() throws Exception {
        //given
        User userE = User.builder()
                .username("Admin")
                .password("Admin")
                .role("ROLE_EMPLOYEE")
                .build();
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        userRepository.save(userE);
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .user(user)
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(patch("/api/v1/books/return/" + book.getId()))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Test")))
                .andExpect(jsonPath("$.author", equalTo("Test")))
                .andExpect(jsonPath("$.state", equalTo(State.READY.toString())));
    }

    @Test
    void testReturnBook_ShouldReturnEntityNotFoundException() throws Exception {
        //given
        User userE = User.builder()
                .username("Admin")
                .password("Admin")
                .role("ROLE_EMPLOYEE")
                .build();
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        userRepository.save(userE);
        //when //then
        mockMvc.perform(patch("/api/v1/books/return/1"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found book with id: 1")));
    }

    @Test
    void testReturnBook_ShouldReturnIllegalArgumentException() throws Exception {
        //given
        User userE = User.builder()
                .username("Admin")
                .password("Admin")
                .role("ROLE_USER")
                .build();
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        userRepository.save(userE);

        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .user(user)
                .build();
        bookRepository.save(book);
        //when //then
        mockMvc.perform(patch("/api/v1/books/return/" + book.getId()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("No access to book with id: "
                        + book.getId())));
    }

    @Test
    void testGetAll() throws Exception {
        //given
        Book book = Book.builder()
                .title("Test")
                .author("Test")
                .state(State.BORROWED)
                .build();
        Book book1 = Book.builder()
                .title("Test")
                .author("Test")
                .state(State.BORROWED)
                .build();
        Book book2 = Book.builder()
                .title("Test")
                .author("Test")
                .state(State.BORROWED)
                .build();
        List<Book> bookList = List.of(book, book1);
        bookRepository.save(book);
        bookRepository.save(book1);
        bookRepository.save(book2);

        //when //then
        mockMvc.perform(get("/api/v1/books")
                        .param("pageNo", "0")
                        .param("pageSize", "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(bookList.size())))
                .andExpect(jsonPath("$[0].title", equalTo("Test")))
                .andExpect(jsonPath("$[0].author", equalTo("Test")))
                .andExpect(jsonPath("$[0].state", equalTo(State.BORROWED.toString())));

    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testGetAllByUser_USER() throws Exception {
        //given
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("Test")
                .author("Test")
                .state(State.BORROWED)
                .user(user)
                .build();
        List<Book> bookList = List.of(book);
        book.setUser(user);
        bookRepository.save(book);

        //when //then
        mockMvc.perform(get("/api/v1/books/user"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(bookList.size())))
                .andExpect(jsonPath("$[0].title", equalTo("Test")))
                .andExpect(jsonPath("$[0].author", equalTo("Test")))
                .andExpect(jsonPath("$[0].state", equalTo(State.BORROWED.toString())));
    }

    @Test
    void testGetAllByUser_EMPLOYEE() throws Exception {
        //given
        User user = User.builder()
                .username("Test")
                .password("Test")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("Test")
                .author("Test")
                .state(State.BORROWED)
                .user(user)
                .build();
        List<Book> bookList = List.of(book);
        book.setUser(user);
        bookRepository.save(book);

        //when //then
        mockMvc.perform(get("/api/v1/books/user")
                        .param("userId", Integer.toString(user.getId())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(bookList.size())))
                .andExpect(jsonPath("$[0].title", equalTo("Test")))
                .andExpect(jsonPath("$[0].author", equalTo("Test")))
                .andExpect(jsonPath("$[0].state", equalTo(State.BORROWED.toString())));
    }
}