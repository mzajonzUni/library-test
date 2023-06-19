package pl.zajonz.librarytest.model;

import lombok.Data;

@Data
public class InfoMessage {

    private int bookId;
    private String book_title;
    private String book_author;
    private String book_category;
    private String email;
    private String user_firstName;
    private String user_lastName;

}
