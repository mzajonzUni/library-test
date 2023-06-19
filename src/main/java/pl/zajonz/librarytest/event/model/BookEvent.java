package pl.zajonz.librarytest.event.model;

import lombok.Data;
import pl.zajonz.librarytest.model.Book;

@Data
public class BookEvent {
    private final Book book;

}
