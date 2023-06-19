package pl.zajonz.librarytest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.zajonz.librarytest.mapper.InfoMessageMapper;
import pl.zajonz.librarytest.mapper.PerformanceInfoMapper;
import pl.zajonz.librarytest.model.Book;
import pl.zajonz.librarytest.model.User;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MessageSenderImpl implements MessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final InfoMessageMapper infoMessageMapper;
    private final PerformanceInfoMapper performanceInfoMapper;

    @Value("${email-info-queue}")
    private String emailQueueName;

    @Value("${performance-info-queue}")
    private String performanceQueueName;

    @Value("${info-queue}")
    private String infoQueueName;

    @Override
    public void sendEmailInfo(Book book) {
        for (User user : book.getCategory().getUsers()) {
            rabbitTemplate.convertAndSend(emailQueueName, infoMessageMapper.toInfoMessage(book, user));
        }
    }

    @Override
    public void sendInfo(String info) {
        rabbitTemplate.convertAndSend(infoQueueName, info);
    }

    @Override
    public void sendPerformanceInfo(User user, long executionTime,
                                    String toShortString, LocalDateTime startMethodDateTime) {
        rabbitTemplate.convertAndSend(performanceQueueName, performanceInfoMapper.toPerformanceInfo(user, executionTime,
                toShortString, startMethodDateTime));
    }
}
