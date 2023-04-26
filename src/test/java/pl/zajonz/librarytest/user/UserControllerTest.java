package pl.zajonz.librarytest.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.zajonz.librarytest.book.BookRepository;
import pl.zajonz.librarytest.book.model.Book;
import pl.zajonz.librarytest.user.model.User;
import pl.zajonz.librarytest.user.model.command.CreateUserCommand;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void clearDatabase() {
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCreate_AllUsers_CorrectValues() throws Exception {
        //given
        CreateUserCommand command = CreateUserCommand.builder()
                .firstname("Firsttest")
                .lastname("Lasttest")
                .password("Testtest")
                .username("Usertest")
                .build();

        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn(command.getPassword());
        //when //then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.username", equalTo(command.getUsername())))
                .andExpect(jsonPath("$.firstname", equalTo(command.getFirstname())))
                .andExpect(jsonPath("$.lastname", equalTo(command.getLastname())));

        User user = userRepository.findByUsername(command.getUsername()).orElse(null);
        assertNotNull(user);
        assertEquals(command.getUsername(), user.getUsername());
        assertEquals(command.getFirstname(), user.getFirstname());
        assertEquals(command.getLastname(), user.getLastname());
        assertEquals(command.getPassword(), user.getPassword());
    }

    @Test
    void testCreate_AllUsers_UsernameAlreadyExists_ShouldThrowIllegalArgumentException() throws Exception {
        //given
        User user = User.builder().
                firstname("Test")
                .lastname("Test")
                .password("TestTest")
                .username("Test")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);

        CreateUserCommand command = CreateUserCommand.builder()
                .firstname("Test")
                .lastname("Test")
                .password("TestTest")
                .username("Test")
                .build();

        //when //then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("User with username: Test already exists")));
    }

    @Test
    void testCreate_AllUsers_ValidationCheck() throws Exception {
        //given
        CreateUserCommand command = CreateUserCommand.builder()
                .firstname("")
                .lastname("")
                .password("")
                .username("")
                .build();

        //when //then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Validation errors")))
                .andExpect(jsonPath("$.violations[*].message",
                        containsInAnyOrder("first name cannot be blank",
                                "user name cannot be blank",
                                "last name cannot be blank",
                                "first name has to match the pattern",
                                "last name has to match the pattern",
                                "password must be 8 characters long")));
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testGetAll_RoleEmployee_CorrectValues() throws Exception {
        //given
        User user = User.builder()
                .firstname("Test")
                .lastname("Test")
                .password("TestTest")
                .username("Test")
                .role("ROLE_USER")
                .build();
        User user1 = User.builder()
                .firstname("Test1")
                .lastname("Test1")
                .password("TestTest1")
                .username("Test1")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        userRepository.save(user1);

        //when //then
        mockMvc.perform(get("/api/v1/users")
                        .param("pageNo", "1")
                        .param("pageSize", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username", equalTo(user.getUsername())))
                .andExpect(jsonPath("$.content[0].lastname", equalTo(user.getLastname())))
                .andExpect(jsonPath("$.numberOfElements", equalTo(1)))
                .andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.totalPages", equalTo(2)));
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testGetAll_RoleEmployee_WrongPageNumber_ShouldThrowIllegalArgumentException() throws Exception {
        //given

        //when //then
        mockMvc.perform(get("/api/v1/users")
                        .param("pageNo", "0")
                        .param("pageSize", "2"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Page index must not be less than zero")));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testGetAll_RoleEmployee_ShouldReturnForbidden() throws Exception {
        //given

        //when //then
        mockMvc.perform(get("/api/v1/users"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testGetAllBooks_RoleEmployee_CorrectValues() throws Exception {
        //given
        User user = User.builder()
                .username("Test")
                .password("Test")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("TitleTest")
                .author("AuthorTest")
                .user(user)
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(get("/api/v1/users/" + user.getId() + "/books"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", equalTo(book.getTitle())))
                .andExpect(jsonPath("$[0].author", equalTo(book.getAuthor())));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testGetAllBooks_RoleUser_CorrectValues() throws Exception {
        //given
        User user = User.builder()
                .username("user")
                .password("user")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("TitleTest")
                .author("AuthorTest")
                .user(user)
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(get("/api/v1/users/" + user.getId() + "/books"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", equalTo(book.getTitle())))
                .andExpect(jsonPath("$[0].author", equalTo(book.getAuthor())));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testGetAllBooks_RoleUser_ShouldReturnIllegalArgumentException() throws Exception {
        //given
        User user = User.builder()
                .username("Test")
                .password("Test")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("TitleTest")
                .author("AuthorTest")
                .user(user)
                .build();
        bookRepository.save(book);

        //when //then
        mockMvc.perform(get("/api/v1/users/" + user.getId() + "/books"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("No access to book with id: " + user.getId())));
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testGetAllBooks_RoleEmployee_ShouldReturnEntityNotFoundException() throws Exception {
        //given

        //when //then
        mockMvc.perform(get("/api/v1/users/100/books"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found user with id: 100")));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testGetAllBooks_RoleUser_ShouldReturnEntityNotFoundException() throws Exception {
        //given

        //when //then
        mockMvc.perform(get("/api/v1/users/100/books"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found user with id: 100")));
    }
}