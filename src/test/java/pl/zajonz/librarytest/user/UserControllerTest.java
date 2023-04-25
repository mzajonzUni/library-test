package pl.zajonz.librarytest.user;

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
import pl.zajonz.librarytest.user.model.User;
import pl.zajonz.librarytest.user.model.command.CreateUserCommand;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "Admin", password = "Admin", roles = "EMPLOYEE")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void clearDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void testCreate_CorrectValues() throws Exception {
        //given
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.username", equalTo("Test")))
                .andExpect(jsonPath("$.firstname", equalTo("Test")));
    }

    @Test
    void testCreate_IncorrectUser() throws Exception {
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
    void testCreate_ValidationCheck() throws Exception {
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
    void testGetAll_CorrectValues() throws Exception {
        //given
        User user = User.builder().
                firstname("Test")
                .lastname("Test")
                .password("TestTest")
                .username("Test")
                .role("ROLE_USER")
                .build();
        userRepository.save(user);

        List<User> userList = List.of(user);

        //when //then
        mockMvc.perform(get("/api/v1/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(userList.size())))
                .andExpect(jsonPath("$.[0]", notNullValue()))
                .andExpect(jsonPath("$.[0].id", equalTo(user.getId())))
                .andExpect(jsonPath("$.[0].username", equalTo("Test")));
    }

    @Test
    @WithMockUser(username = "user", password = "user", roles = "USER")
    void testGetAll_ShouldReturnForbidden() throws Exception {
        //given

        //when //then
        mockMvc.perform(get("/api/v1/users"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}