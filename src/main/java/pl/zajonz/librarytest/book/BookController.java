package pl.zajonz.librarytest.book;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.zajonz.librarytest.book.model.Book;
import pl.zajonz.librarytest.book.model.BookDto;
import pl.zajonz.librarytest.book.model.command.CreateBookCommand;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookDto create(@RequestBody @Valid CreateBookCommand command) {
        Book book = command.toEntity();
        return BookDto.fromEntity(bookService.create(book));
    }

    @PatchMapping("/block/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BookDto blockBook(@PathVariable int id) {
        return BookDto.fromEntity(bookService.blockBook(id));
    }

    @PutMapping("/borrow/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BookDto borrowBook(Principal principal, @PathVariable int id, @RequestParam LocalDate to) {
        return BookDto.fromEntity(bookService.borrowBook(principal.getName(), id, to));
    }

    @PatchMapping("/return/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BookDto returnBook(Principal principal, @PathVariable int id) {
        return BookDto.fromEntity(bookService.returnBook(principal.getName(), id));
    }

    @GetMapping
    public List<BookDto> getAll(@RequestParam(required = false, defaultValue = "0") int pageNo,
                                @RequestParam(required = false, defaultValue = "50") int pageSize) {
        return bookService.getAll(pageNo, pageSize).stream()
                .map(BookDto::fromEntity)
                .toList();
    }

    @GetMapping("/user")
    public List<BookDto> getAllByUser(Authentication auth, @RequestParam(required = false, defaultValue = "1") int userId) {
        return bookService.getAllByUser(auth.getName(), auth.getAuthorities().toString(), userId).stream()
                .map(BookDto::fromEntity)
                .toList();
    }


}
