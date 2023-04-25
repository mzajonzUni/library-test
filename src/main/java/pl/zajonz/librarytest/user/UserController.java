package pl.zajonz.librarytest.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.zajonz.librarytest.user.model.User;
import pl.zajonz.librarytest.user.model.UserDto;
import pl.zajonz.librarytest.user.model.command.CreateUserCommand;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody @Valid CreateUserCommand command) {
        User user = command.toEntity();
        return UserDto.fromEntity(userService.create(user));
    }

    @GetMapping
    public List<UserDto> getAll(@RequestParam(required = false, defaultValue = "0") int pageNo,
                                @RequestParam(required = false, defaultValue = "50") int pageSize) {
        return userService.getAll(pageNo, pageSize).stream()
                .map(UserDto::fromEntity)
                .toList();
    }

}
