package pl.zajonz.librarytest.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pl.zajonz.librarytest.model.DTO.UserDto;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.model.command.CreateUserCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void testToUserEntity_whenMaps_thenCorrect() {
        //given
        CreateUserCommand command = CreateUserCommand.builder()
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test.pl")
                .password("Test")
                .role("ROLE_TEST")
                .build();
        //when
        User returned = userMapper.toUserEntity(command);

        //then
        assertEquals(command.getPassword(), returned.getPassword());
        assertEquals(command.getUsername(), returned.getUsername());
        assertEquals(command.getFirstname(), returned.getFirstname());
        assertEquals(command.getLastname(), returned.getLastname());
        assertEquals(command.getEmail(), returned.getEmail());
        assertEquals(command.getRole(), returned.getRole());
        assertNull(returned.getBooks());
        assertNull(returned.getSubscribedCategories());
    }

    @Test
    void fromUserEntity_whenMaps_thenCorrect() {
        //given
        User user = User.builder()
                .id(1)
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test.pl")
                .role("ROLE_TEST")
                .password("TEST")
                .build();
        //when
        UserDto returned = userMapper.fromUserEntity(user);

        //then
        assertEquals(user.getId(), returned.getId());
        assertEquals(user.getUsername(), returned.getUsername());
        assertEquals(user.getFirstname(), returned.getFirstname());
        assertEquals(user.getLastname(), returned.getLastname());
        assertEquals(user.getEmail(), returned.getEmail());
    }
}