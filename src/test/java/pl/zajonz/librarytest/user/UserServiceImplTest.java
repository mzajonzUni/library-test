package pl.zajonz.librarytest.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.zajonz.librarytest.user.model.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

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
    }

    @Test
    void testGetAll_CorrectValues_ResultsInListUserBeingReturned() {
        //given
        User user = User.builder()
                .username("Test")
                .password("Test")
                .build();
        List<User> userList = List.of(user);
        Page<User> userPage = new PageImpl<>(userList);

        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);
        //when
        List<User> returned = userService.getAll(0, 1);
        //then
        assertEquals(userList, returned);
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