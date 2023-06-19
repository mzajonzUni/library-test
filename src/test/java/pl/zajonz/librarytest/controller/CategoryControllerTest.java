package pl.zajonz.librarytest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.zajonz.librarytest.model.Category;
import pl.zajonz.librarytest.model.PerformanceInfo;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.repository.CategoryRepository;
import pl.zajonz.librarytest.repository.UserRepository;
import pl.zajonz.librarytest.service.MessageSender;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @SpyBean
    private MessageSender messageSender;
    @MockBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    public void clearDatabase() {
        categoryRepository.deleteAll();
    }


    @Test
    void testGetCategories() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        Category category2 = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);
        categoryRepository.save(category2);

        //when //then
        mockMvc.perform(get("/api/v1/categories"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id", equalTo(category.getId())))
                .andExpect(jsonPath("$[0].name", equalTo(category.getName())))
                .andExpect(jsonPath("$[1].id", equalTo(category2.getId())))
                .andExpect(jsonPath("$[1].name", equalTo(category2.getName())));

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class), any(Long.class),
                anyString(), any(LocalDateTime.class));
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testSubscribe_CorrectValues_ShouldUpdateSubscribedCategories() throws Exception {
        //given
        User user = User.builder()
                .firstname("TestF")
                .lastname("test")
                .username("user")
                .email("test@test.pl")
                .role("ROLE_CUSTOMER")
                .subscribedCategories(new HashSet<>())
                .password("user")
                .build();
        userRepository.save(user);
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        //when //then
        mockMvc.perform(patch("/api/v1/categories/" + category.getId() + "/subscribe"))
                .andDo(print())
                .andExpect(status().isAccepted());

        User updatedUser = userRepository.findByUsername(user.getUsername()).orElse(null);
        assert updatedUser != null;
        assertTrue(updatedUser.getSubscribedCategories().contains(category));

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(1)).sendPerformanceInfo(any(User.class), any(Long.class),
                anyString(), any(LocalDateTime.class));
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testSubscribe_IncorrectCategoryId_ShouldThrowEntityNotFoundException() throws Exception {
        //given
        User user = User.builder()
                .firstname("TestF")
                .lastname("test")
                .username("user")
                .email("test@test.pl")
                .role("ROLE_CUSTOMER")
                .subscribedCategories(new HashSet<>())
                .password("user")
                .build();
        userRepository.save(user);
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        //when //then
        mockMvc.perform(patch("/api/v1/categories/100/subscribe"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found category with id: 100")));

        User updatedUser = userRepository.findByUsername(user.getUsername()).orElse(null);
        assert updatedUser != null;
        assertFalse(updatedUser.getSubscribedCategories().contains(category));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class), any(Long.class),
                anyString(), any(LocalDateTime.class));
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", password = "user", roles = "CUSTOMER")
    void testSubscribe_IncorrectUser_ShouldThrowEntityNotFoundException() throws Exception {
        //given
        Category category = Category.builder()
                .name("test")
                .build();
        categoryRepository.save(category);

        //when //then
        mockMvc.perform(patch("/api/v1/categories/1/subscribe"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.message", equalTo("Not found user with username: user")));

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class), any(Long.class),
                anyString(), any(LocalDateTime.class));
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", password = "admin", roles = "EMPLOYEE")
    void testSubscribe_Employee_ShouldReturnUnauthorized() throws Exception {
        //given
        User user = User.builder()
                .firstname("TestF")
                .lastname("test")
                .username("admin")
                .email("test@test.pl")
                .role("ROLE_EMPLOYEE")
                .subscribedCategories(new HashSet<>())
                .password("admin")
                .build();
        userRepository.save(user);

        //when //then
        mockMvc.perform(patch("/api/v1/categories/1/subscribe"))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), anyString());
        verify(messageSender, times(0)).sendInfo(anyString());
        verify(rabbitTemplate, times(0)).convertAndSend(anyString(), any(PerformanceInfo.class));
        verify(messageSender, times(0)).sendPerformanceInfo(any(User.class), any(Long.class),
                anyString(), any(LocalDateTime.class));
    }
}