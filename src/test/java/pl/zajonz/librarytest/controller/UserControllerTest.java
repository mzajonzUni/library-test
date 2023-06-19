package pl.zajonz.librarytest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.model.Book;
import pl.zajonz.librarytest.model.Category;
import pl.zajonz.librarytest.model.PerformanceInfo;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.model.command.CreateUserCommand;
import pl.zajonz.librarytest.repository.BookRepository;
import pl.zajonz.librarytest.repository.CategoryRepository;
import pl.zajonz.librarytest.repository.UserRepository;
import pl.zajonz.librarytest.service.MessageSender;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
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
    private CategoryRepository categoryRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private RabbitTemplate rabbitTemplate;
    @SpyBean
    private MessageSender messageSender;

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
                .email("test@test.pl")
                .role("ROLE_CUSTOMER")
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

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(1)).sendInfo(anyString());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    void testCreate_AllUsers_EmailAlreadyExists_ShouldThrowIllegalArgumentException() throws Exception {
        //given
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

        CreateUserCommand command = CreateUserCommand.builder()
                .firstname("Test")
                .lastname("Test")
                .password("TestTest")
                .username("Test")
                .email("test@test.pl")
                .role("ROLE_CUSTOMER")
                .build();

        //when //then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("User with email: " + command.getEmail() + " already exists")));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    void testCreate_AllUsers_UsernameAlreadyExists_ShouldThrowIllegalArgumentException() throws Exception {
        //given
        User user = User.builder()
                .username("Test")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test.pl")
                .role("ROLE_TEST")
                .password("user")
                .locked(false)
                .build();
        userRepository.save(user);

        CreateUserCommand command = CreateUserCommand.builder()
                .firstname("Test")
                .lastname("Test")
                .password("TestTest")
                .username("Test")
                .email("test2@test.pl")
                .role("ROLE_CUSTOMER")
                .build();

        //when //then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("User with username: Test already exists")));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    void testCreate_AllUsers_ValidationCheck() throws Exception {
        //given
        CreateUserCommand command = CreateUserCommand.builder()
                .firstname("")
                .lastname("")
                .password("")
                .username("")
                .role("")
                .email("")
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
                                "password must be 8 characters long",
                                "role cannot be blank",
                                "role has to match the pattern",
                                "email cannot be blank")));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
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
                .role("ROLE_CUSTOMER")
                .email("test1@test.pl")
                .build();
        User user1 = User.builder()
                .firstname("Test1")
                .lastname("Test1")
                .password("TestTest1")
                .username("Test1")
                .role("ROLE_CUSTOMER")
                .email("test@test.pl")
                .build();
        userRepository.save(user);
        userRepository.save(user1);

        //when //then
        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username", equalTo(user.getUsername())))
                .andExpect(jsonPath("$.content[0].lastname", equalTo(user.getLastname())))
                .andExpect(jsonPath("$.numberOfElements", equalTo(1)))
                .andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.totalPages", equalTo(2)));

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testGetAll_RoleEmployee_ShouldReturnForbidden() throws Exception {
        //given

        //when //then
        mockMvc.perform(get("/api/v1/users"))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
    void testGetAllBooks_RoleEmployee_CorrectValues() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);
        User user = User.builder()
                .firstname("Test")
                .lastname("Test")
                .password("TestTest")
                .username("Test")
                .role("ROLE_EMPLOYEE")
                .email("test1@test.pl")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(false)
                .user(user)
                .state(State.READY)
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

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testGetAllBooks_RoleUser_CorrectValues() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);
        User user = User.builder()
                .firstname("Test")
                .lastname("Test")
                .password("user")
                .username("user")
                .role("ROLE_CUSTOMER")
                .email("test1@test.pl")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(false)
                .user(user)
                .state(State.READY)
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

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testGetAllBooks_RoleUser_ShouldReturnIllegalArgumentException() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);
        User user = User.builder()
                .firstname("Test")
                .lastname("Test")
                .password("Test")
                .username("Test")
                .role("ROLE_CUSTOMER")
                .email("test1@test.pl")
                .build();
        userRepository.save(user);
        Book book = Book.builder()
                .title("Test")
                .author("Testowy")
                .category(category)
                .isBlocked(false)
                .user(user)
                .state(State.READY)
                .build();
        bookRepository.save(book);


        //when //then
        mockMvc.perform(get("/api/v1/users/" + user.getId() + "/books"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("No access to book with id: " + user.getId())));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
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

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testGetAllBooks_RoleUser_ShouldReturnEntityNotFoundException() throws Exception {
        //given

        //when //then
        mockMvc.perform(get("/api/v1/users/100/books"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found user with id: 100")));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class),any(Long.class),
                anyString(),any(LocalDateTime.class));
    }
}