package pl.zajonz.librarytest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.zajonz.librarytest.adnotation.MonitorMethod;
import pl.zajonz.librarytest.mapper.BookMapper;
import pl.zajonz.librarytest.mapper.UserMapper;
import pl.zajonz.librarytest.model.DTO.BookDto;
import pl.zajonz.librarytest.model.DTO.UserDto;
import pl.zajonz.librarytest.model.User;
import pl.zajonz.librarytest.model.command.CreateUserCommand;
import pl.zajonz.librarytest.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @MonitorMethod
    public UserDto create(@RequestBody @Valid CreateUserCommand command) {
        User user = userMapper.toUserEntity(command);
        return userMapper.fromUserEntity(userService.create(user));
    }

    @GetMapping
    @MonitorMethod
    public Page<UserDto> getAll(@PageableDefault Pageable pageable) {
        return userService.getAll(pageable).map(userMapper::fromUserEntity);
    }

    @GetMapping("/{id}/books")
    @MonitorMethod
    public List<BookDto> getAllBooks(Authentication auth, @PathVariable int id) {
        return userService.getAllBooks(auth.getName(), auth.getAuthorities().toString(), id)
                .stream()
                .map(bookMapper::fromBookEntity)
                .toList();

    }

}
