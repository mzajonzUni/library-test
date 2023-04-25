package pl.zajonz.librarytest.user.model.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import pl.zajonz.librarytest.user.model.User;

@Data
@Builder
public class CreateUserCommand {

    @NotBlank(message = "first name cannot be blank")
    @Pattern(regexp = "[A-Z][a-z]{1,20}", message = "first name has to match the pattern")
    private String firstname;
    @NotBlank(message = "last name cannot be blank")
    @Pattern(regexp = "[A-Z][a-z]{1,20}", message = "last name has to match the pattern")
    private String lastname;

    @NotBlank(message = "user name cannot be blank")
    private String username;

    @Pattern(regexp = ".{8,}", message = "password must be 8 characters long")
    private String password;

    public User toEntity() {
        return User.builder()
                .firstname(firstname)
                .lastname(lastname)
                .username(username)
                .password(password)
                .build();
    }


}
