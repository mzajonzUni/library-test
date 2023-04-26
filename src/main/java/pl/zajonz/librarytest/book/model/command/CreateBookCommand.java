package pl.zajonz.librarytest.book.model.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import pl.zajonz.librarytest.book.model.Book;

@Data
@Builder
public class CreateBookCommand {

    @NotBlank(message = "title cannot be blank")
    @Pattern(regexp = "[A-Z][a-z]{1,40}", message = "title has to match the pattern")
    private String title;

    @NotBlank(message = "author cannot be blank")
    @Pattern(regexp = "[A-Z][a-z]{1,40}", message = "author has to match the pattern")
    private String author;

    public Book toEntity() {
        return Book.builder()
                .title(title)
                .author(author)
                .build();
    }

}
