package pl.zajonz.librarytest.user.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDto {

    private int id;
    private String firstname;
    private String lastname;
    private String username;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .username(user.getUsername())
                .build();
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "username='" + username + '\'' +
                '}';
    }
}
