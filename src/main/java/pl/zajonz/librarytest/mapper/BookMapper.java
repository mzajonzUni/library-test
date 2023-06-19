package pl.zajonz.librarytest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.zajonz.librarytest.model.Book;
import pl.zajonz.librarytest.model.DTO.BookDto;
import pl.zajonz.librarytest.model.command.CreateBookCommand;

@Mapper(componentModel = ("spring"))
public interface BookMapper {

    @Mapping(source = "book.category.name", target = "category")
    @Mapping(source = "book.user.username", target = "username")
    BookDto fromBookEntity(Book book);

    Book toBookEntity(CreateBookCommand command);

}
