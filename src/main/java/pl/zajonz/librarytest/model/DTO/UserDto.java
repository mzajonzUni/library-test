package pl.zajonz.librarytest.model.DTO;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class UserDto {

    private int id;
    private String firstname;
    private String lastname;
    private String username;
    private String email;

}
