package pl.zajonz.librarytest.mapper;

import org.mapstruct.Mapper;
import pl.zajonz.librarytest.model.DTO.UserDto;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.model.command.CreateUserCommand;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUserEntity(CreateUserCommand command);

    UserDto fromUserEntity(User user);

}
