package pl.zajonz.librarytest.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import pl.zajonz.librarytest.event.model.InfoEvent;
import pl.zajonz.librarytest.model.Category;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.repository.CategoryRepository;
import pl.zajonz.librarytest.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CategoryServiceImplTest {

    @InjectMocks
    private CategoryServiceImpl categoryService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Captor
    private ArgumentCaptor<User> argumentCaptor;

    @Test
    void testSubscribe_CorrectValues() {
        //given
        User user = User.builder()
                .firstname("TestF")
                .username("Test")
                .password("Test")
                .subscribedCategories(new HashSet<>())
                .build();
        Category category = Category.builder()
                .id(1)
                .name("test")
                .build();

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(anyInt())).thenReturn(Optional.of(category));
        //when
        categoryService.subscribe(user.getUsername(), category.getId());

        //then
        verify(userRepository, times(1)).save(argumentCaptor.capture());
        User captured = argumentCaptor.getValue();
        assertEquals(user.getFirstname(), captured.getFirstname());
        assertEquals(user.getUsername(), captured.getUsername());
        assertEquals(user.getSubscribedCategories(), captured.getSubscribedCategories());

        verify(eventPublisher, times(1)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testSubscribe_InCorrectUsername_ResultsInEntityNotFoundException() {
        //given
        User user = User.builder()
                .firstname("TestF")
                .username("Test")
                .password("Test")
                .subscribedCategories(new HashSet<>())
                .build();
        String exceptionMsg = "Not found user with username: " + user.getUsername();

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        //when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> categoryService.subscribe(user.getUsername(), 1));
        //then
        assertEquals(exceptionMsg, exception.getMessage());
        verify(userRepository, times(0)).save(any(User.class));
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testSubscribe_InCorrectEmail_ResultsInEntityNotFoundException() {
        //given
        User user = User.builder()
                .firstname("TestF")
                .username("Test")
                .password("Test")
                .subscribedCategories(new HashSet<>())
                .build();
        String exceptionMsg = "Not found category with id: 1";

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(anyInt())).thenReturn(Optional.empty());
        //when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> categoryService.subscribe(user.getUsername(), 1));
        //then
        assertEquals(exceptionMsg, exception.getMessage());
        verify(userRepository, times(0)).save(argumentCaptor.capture());
        verify(eventPublisher, times(0)).publishEvent(any(InfoEvent.class));
    }

    @Test
    void testGetCategories_ResultsInListCategoriesBeingReturned() {
        //given
        Category category = Category.builder()
                .id(1)
                .name("test")
                .build();
        Category category2 = Category.builder()
                .id(2)
                .name("test")
                .build();
        List<Category> categories = List.of(category, category2);

        when(categoryRepository.findAll()).thenReturn(categories);
        //when
        List<Category> returned = categoryService.getCategories();

        //then
        assertEquals(categories, returned);
    }
}