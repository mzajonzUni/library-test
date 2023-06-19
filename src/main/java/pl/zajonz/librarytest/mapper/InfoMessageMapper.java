package pl.zajonz.librarytest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.zajonz.librarytest.model.Book;
import pl.zajonz.librarytest.model.InfoMessage;
import pl.zajonz.librarytest.model.User;

@Mapper(componentModel = "spring")
public interface InfoMessageMapper {

    @Mapping(source = "book.id", target = "bookId")
    @Mapping(source = "book.title", target = "book_title")
    @Mapping(source = "book.author", target = "book_author")
    @Mapping(source = "book.category.name", target = "book_category")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.firstname", target = "user_firstName")
    @Mapping(source = "user.lastname", target = "user_lastName")
    InfoMessage toInfoMessage(Book book, User user);

}
