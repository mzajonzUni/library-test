package pl.zajonz.librarytest.book.model;

import lombok.Builder;
import lombok.Getter;
import pl.zajonz.librarytest.common.State;

import java.time.LocalDate;

@Getter
@Builder
public class BookDto {

    private int id;
    private String title;
    private String author;
    private boolean isBlocked;
    private State state;
    private String userName;
    private LocalDate fromDate;
    private LocalDate toDate;

    public static BookDto fromEntity(Book book) {
        return BookDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isBlocked(book.isBlocked())
                .userName(book.getUser() == null ? null : book.getUser().getUsername())
                .state(book.getState())
                .fromDate(book.getFromDate())
                .toDate(book.getToDate())
                .build();
    }

}
