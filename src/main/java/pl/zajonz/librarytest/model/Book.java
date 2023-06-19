package pl.zajonz.librarytest.model;

import jakarta.persistence.*;
import lombok.*;
import pl.zajonz.librarytest.common.State;

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
    private Category category;
    @ManyToOne
    private User user;

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", isBlocked=" + isBlocked +
                ", state=" + state +
                '}';
    }
}
