package pl.zajonz.librarytest.model.DTO;

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
    private String username;
    private String category;
    private boolean isBlocked;
    private State state;
    private LocalDate fromDate;
    private LocalDate toDate;

}
