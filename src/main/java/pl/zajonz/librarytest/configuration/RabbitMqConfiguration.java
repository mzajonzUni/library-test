package pl.zajonz.librarytest.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfiguration {

    @Value("${email-info-queue}")
    private String emailInfoQueueName;
    @Value("${performance-info-queue}")
    private String performanceInfoQueueName;
    @Value("${info-queue}")
    private String infoQueueName;

    @Bean
    Queue emailInfoQueue() {
        return new Queue(emailInfoQueueName, false);
    }

    @Bean
    Queue performanceInfoQueue() {
        return new Queue(performanceInfoQueueName, false);
    }

    @Bean
    Queue databaseInfoQueue() {
        return new Queue(infoQueueName, false);
    }

    @Bean
    public MessageConverter converter() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

}
