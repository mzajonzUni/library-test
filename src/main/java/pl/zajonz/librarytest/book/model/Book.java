package pl.zajonz.librarytest.book.model;

import jakarta.persistence.*;
import lombok.*;
import pl.zajonz.librarytest.common.State;
import pl.zajonz.librarytest.user.model.User;

import java.time.LocalDate;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String title;
    private String author;
    private boolean isBlocked;
    private State state;
    private LocalDate fromDate;
    private LocalDate toDate;
    @ManyToOne
    private User user;

}
