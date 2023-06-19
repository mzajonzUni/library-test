package pl.zajonz.librarytest.model.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateBookCommand {

    @NotBlank(message = "title cannot be blank")
    @Pattern(regexp = "[A-Z][a-z]{1,40}", message = "title has to match the pattern")
    private String title;

    @NotBlank(message = "author cannot be blank")
    @Pattern(regexp = "[A-Z][a-z]{1,40}", message = "author has to match the pattern")
    private String author;

    @NotNull(message = "category cannot be null")
    @Min(1)
    private int categoryId;

}
