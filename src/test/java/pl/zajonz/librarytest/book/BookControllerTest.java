package pl.zajonz.librarytest.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.zajonz.librarytest.book.model.Book;
import pl.zajonz.librarytest.book.model.command.CreateBookCommand;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.user.UserRepository;
import pl.zajonz.librarytest.user.model.User;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
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
    private ObjectMapper objectMapper;

    @BeforeEach
    public void clearDatabase() {
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testCreate_RoleEmployee_CorrectValues() throws Exception {
        //given
        CreateBookCommand command = CreateBookCommand.builder()
                .title("Titletest")
                .author("Authortest")
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
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
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
                                "author has to match the pattern")));
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testBlockBook_RoleEmployee_CorrectValues() throws Exception {
        //given
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(patch("/api/v1/books/" + book.getId() + "/block"))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Test")))
                .andExpect(jsonPath("$.author", equalTo("Test")))
                .andExpect(jsonPath("$.blocked", equalTo(true)));

        Book savedBook = bookRepository.findById(book.getId()).orElse(null);
        assertNotNull(savedBook);
        assertEquals(book.getAuthor(), savedBook.getAuthor());
        assertEquals(book.getTitle(), savedBook.getTitle());
        assertTrue(savedBook.isBlocked());
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBlockBook_RoleUser_ShouldReturnForbidden() throws Exception {
        //given

        //when //then
        mockMvc.perform(patch("/api/v1/books/1/block"))
                .andDo(print())
                .andExpect(status().isForbidden());
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
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBorrowBook_RoleUser_CorrectValues() throws Exception {
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
        mockMvc.perform(put("/api/v1/books/" + book.getId() + "/borrow")
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Test")))
                .andExpect(jsonPath("$.author", equalTo("Test")))
                .andExpect(jsonPath("$.username", equalTo(user.getUsername())))
                .andExpect(jsonPath("$.state", equalTo(State.BORROWED.toString())));

        Book savedBook = bookRepository.findById(book.getId()).orElse(null);
        assertNotNull(savedBook);
        assertEquals(LocalDate.now().plusDays(10), savedBook.getToDate());
        assertEquals(user.getUsername(), savedBook.getUser().getUsername());
        assertNotNull(savedBook.getFromDate());
        assertEquals(State.BORROWED, savedBook.getState());
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
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBorrowBook_RoleUser_IncorrectToDate_ShouldThrowIllegalArgumentException() throws Exception {
        //given

        //when //then
        mockMvc.perform(put("/api/v1/books/1/borrow")
                        .param("to", LocalDate.now().minusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Date 'to' cannot be before today's date")));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBorrowBook_RoleUser_BookNotFound_ShouldThrowEntityNotFoundException() throws Exception {
        //given

        //when //then
        mockMvc.perform(put("/api/v1/books/100/borrow")
                        .param("to", LocalDate.now().plusDays(10).toString()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found book with id: 100")));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBorrowBook_RoleUser_BookBlocked_ShouldThrowIllegalArgumentException() throws Exception {
        //given
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .state(State.READY)
                .isBlocked(true)
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
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testBorrowBook_RoleUser_BookBorrowed_ShouldThrowIllegalArgumentException() throws Exception {
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
                .user(user)
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
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testReturnBook_RoleUser_CorrectValues() throws Exception {
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
        mockMvc.perform(patch("/api/v1/books/" + book.getId() + "/return"))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Test")))
                .andExpect(jsonPath("$.author", equalTo("Test")))
                .andExpect(jsonPath("$.state", equalTo(State.READY.toString())));

        Book returnedBook = bookRepository.findById(book.getId()).orElse(null);
        assertNotNull(returnedBook);
        assertEquals(book.getAuthor(), returnedBook.getAuthor());
        assertEquals(book.getTitle(), returnedBook.getTitle());
        assertNull(returnedBook.getUser());
        assertNull(returnedBook.getToDate());
        assertNull(returnedBook.getFromDate());
        assertEquals(State.READY, returnedBook.getState());
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testReturnBook_RoleEmployee_CorrectValues() throws Exception {
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
        mockMvc.perform(patch("/api/v1/books/" + book.getId() + "/return"))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Test")))
                .andExpect(jsonPath("$.author", equalTo("Test")))
                .andExpect(jsonPath("$.state", equalTo(State.READY.toString())));

        Book returnedBook = bookRepository.findById(book.getId()).orElse(null);
        assertNotNull(returnedBook);
        assertEquals(book.getAuthor(), returnedBook.getAuthor());
        assertEquals(book.getTitle(), returnedBook.getTitle());
        assertNull(returnedBook.getUser());
        assertNull(returnedBook.getToDate());
        assertNull(returnedBook.getFromDate());
        assertEquals(State.READY, returnedBook.getState());
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testReturnBook_RoleUser_BookNotFound_ShouldReturnEntityNotFoundException() throws Exception {
        //given

        //when //then
        mockMvc.perform(patch("/api/v1/books/1/return"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found book with id: 1")));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testReturnBook_RoleUser_AccessDenied_ShouldReturnIllegalArgumentException() throws Exception {
        //given
        User user = User.builder()
                .username("Admin")
                .password("Admin")
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
        mockMvc.perform(patch("/api/v1/books/" + book.getId() + "/return"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("No access to book with id: "
                        + book.getId())));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testReturnBook_RoleUser_BookNotBorrowed_ShouldReturnIllegalArgumentException() throws Exception {
        //given
        Book book = Book.builder()
                .author("Test")
                .title("Test")
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(patch("/api/v1/books/" + book.getId() + "/return"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Book with id: " +
                        book.getId() + " is not borrowed")));
    }

    @Test
    void testGetAll_AllUsers_CorrectValues() throws Exception {
        //given
        Book book = Book.builder()
                .title("Test1")
                .author("Test1")
                .state(State.BORROWED)
                .build();
        Book book1 = Book.builder()
                .title("Test2")
                .author("Test2")
                .state(State.READY)
                .build();
        Book book2 = Book.builder()
                .title("Test3")
                .author("Test3")
                .state(State.READY)
                .build();
        bookRepository.save(book);
        bookRepository.save(book1);
        bookRepository.save(book2);

        //when //then
        mockMvc.perform(get("/api/v1/books")
                        .param("pageNo", "1")
                        .param("pageSize", "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", equalTo(book.getTitle())))
                .andExpect(jsonPath("$.content[0].author", equalTo(book.getAuthor())))
                .andExpect(jsonPath("$.content[1].title", equalTo(book1.getTitle())))
                .andExpect(jsonPath("$.content[1].title", equalTo(book1.getAuthor())))
                .andExpect(jsonPath("$.numberOfElements", equalTo(2)))
                .andExpect(jsonPath("$.size", equalTo(2)))
                .andExpect(jsonPath("$.totalPages", equalTo(2)));
    }

    @Test
    void testGetAll_AllUsers_WrongPageNumber_ShouldThrowIllegalArgumentException() throws Exception {
        //given

        //when //then
        mockMvc.perform(get("/api/v1/books")
                        .param("pageNo", "0")
                        .param("pageSize", "2"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Page index must not be less than zero")));
    }

}