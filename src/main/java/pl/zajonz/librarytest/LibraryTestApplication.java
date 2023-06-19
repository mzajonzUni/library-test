package pl.zajonz.librarytest;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableRabbit
@EnableAspectJAutoProxy
public class LibraryTestApplication{

    public static void main(String[] args) {
        SpringApplication.run(LibraryTestApplication.class, args);
    }
}
