package pl.zajonz.librarytest.service;

import pl.zajonz.librarytest.model.Book;
import pl.zajonz.librarytest.model.User;

import java.time.LocalDateTime;

public interface MessageSender {
    void sendEmailInfo(Book book);

    void sendPerformanceInfo(User user, long executionTime, String toShortString, LocalDateTime startMethodDateTime);

    void sendInfo(String info);
}
