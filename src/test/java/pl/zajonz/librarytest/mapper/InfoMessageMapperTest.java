package pl.zajonz.librarytest.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.model.Book;
import pl.zajonz.librarytest.model.Category;
import pl.zajonz.librarytest.model.InfoMessage;
import pl.zajonz.librarytest.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class InfoMessageMapperTest {

    @Autowired
    private InfoMessageMapper mapper;

    @Test
    void testToInfoMessage() {
        //given
        Category category = Category.builder()
                .name("Testowy")
                .build();
        User user = User.builder()
                .id(1)
                .username("Test123")
                .firstname("Test")
                .lastname("Testowy")
                .email("test@test.pl")
                .role("ROLE_TEST")
                .password("TEST")
                .build();
        Book book = Book.builder()
                .id(1)
                .title("Test")
                .author("Testowy")
                .user(user)
                .category(category)
                .isBlocked(false)
                .state(State.BORROWED)
                .fromDate(LocalDate.now())
                .toDate(LocalDate.now().plusDays(10))
                .build();
        //when
        InfoMessage returned = mapper.toInfoMessage(book, user);

        //then
        assertEquals(book.getId(),returned.getBookId());
        assertEquals(book.getTitle(),returned.getBook_title());
        assertEquals(book.getAuthor(),returned.getBook_author());
        assertEquals(book.getCategory().getName(),returned.getBook_category());
        assertEquals(user.getFirstname(),returned.getUser_firstName());
        assertEquals(user.getLastname(),returned.getUser_lastName());
        assertEquals(user.getEmail(),returned.getEmail());
    }
}