package pl.zajonz.librarytest.book.model.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import pl.zajonz.librarytest.book.model.Book;

@Data
@Builder
public class CreateBookCommand {

    @NotBlank(message = "title name cannot be blank")
    private String title;

    @NotBlank(message = "author name cannot be blank")
    private String author;

    public Book toEntity() {
        return Book.builder()
                .title(title)
                .author(author)
                .build();
    }

}
