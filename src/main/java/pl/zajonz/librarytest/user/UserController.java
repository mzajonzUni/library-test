package pl.zajonz.librarytest.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.zajonz.librarytest.book.model.BookDto;
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
    public Page<UserDto> getAll(@RequestParam(required = false, defaultValue = "1") int pageNo,
                                @RequestParam(required = false, defaultValue = "50") int pageSize) {
        return userService.getAll(pageNo, pageSize).map(UserDto::fromEntity);
    }

    @GetMapping("/{id}/books")
    public List<BookDto> getAllBooks(Authentication auth, @PathVariable int id) {
        return userService.getAllBooks(auth.getName(), auth.getAuthorities().toString(), id)
                .stream()
                .map(BookDto::fromEntity)
                .toList();

    }

}
