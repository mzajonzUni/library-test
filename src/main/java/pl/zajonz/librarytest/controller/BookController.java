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
import pl.zajonz.librarytest.model.DTO.BookDto;
import pl.zajonz.librarytest.model.command.CreateBookCommand;
import pl.zajonz.librarytest.service.BookService;

import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequestMapping("api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookMapper bookMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @MonitorMethod
    public BookDto create(@RequestBody @Valid CreateBookCommand command) {
        return bookMapper.fromBookEntity(bookService.create(
                bookMapper.toBookEntity(command), command.getCategoryId()));
    }

    @PatchMapping("/{id}/block")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @MonitorMethod
    public BookDto blockBook(@PathVariable int id) {
        return bookMapper.fromBookEntity(bookService.blockBook(id));
    }

    @PutMapping("/{id}/borrow")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @MonitorMethod
    public BookDto borrowBook(Principal principal, @PathVariable int id, @RequestParam LocalDate to) {
        return bookMapper.fromBookEntity(bookService.borrowBook(principal.getName(), id, to));
    }

    @PatchMapping("/{id}/return")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @MonitorMethod
    public BookDto returnBook(Authentication auth, @PathVariable int id) {
        return bookMapper.fromBookEntity(bookService.returnBook(auth.getName(), auth.getAuthorities().toString(), id));
    }

    @GetMapping
    @MonitorMethod
    public Page<BookDto> getAll(@PageableDefault Pageable pageable) {
        return bookService.getAll(pageable).map(bookMapper::fromBookEntity);
    }

}
