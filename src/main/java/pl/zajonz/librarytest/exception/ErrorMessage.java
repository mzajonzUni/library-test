package pl.zajonz.librarytest.exception;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ErrorMessage {

    private LocalDateTime timestamp;
    private String message;

    public ErrorMessage(String message) {
        this.timestamp = LocalDateTime.now();
        this.message = message;
    }
}
