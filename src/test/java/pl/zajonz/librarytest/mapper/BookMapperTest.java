package pl.zajonz.librarytest.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.model.Book;
import pl.zajonz.librarytest.model.Category;
import pl.zajonz.librarytest.model.DTO.BookDto;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.model.command.CreateBookCommand;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BookMapperTest {

    @Autowired
    private BookMapper bookMapper;

    @Test
    void testFromBookEntity() {
        //given
        User user = User.builder().username("Tescik").build();
        Category category = Category.builder().name("Testowy").build();
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
        BookDto returned = bookMapper.fromBookEntity(book);

        //then
        assertEquals(book.getId(),returned.getId());
        assertEquals(book.getTitle(),returned.getTitle());
        assertEquals(book.getAuthor(),returned.getAuthor());
        assertEquals(book.getUser().getUsername(),returned.getUsername());
        assertEquals(book.getCategory().getName(),returned.getCategory());
        assertEquals(book.getFromDate(),returned.getFromDate());
        assertEquals(book.getToDate(),returned.getToDate());
    }

    @Test
    void testToBookEntity() {
        //given
        CreateBookCommand command = CreateBookCommand.builder()
                .title("Test")
                .author("Testowy")
                .categoryId(1)
                .build();
        //when
        Book returned = bookMapper.toBookEntity(command);

        //then
        assertEquals(command.getTitle(),returned.getTitle());
        assertEquals(command.getAuthor(),returned.getAuthor());
        assertNull(returned.getCategory());
        assertNull(returned.getFromDate());
        assertNull(returned.getToDate());
    }
}