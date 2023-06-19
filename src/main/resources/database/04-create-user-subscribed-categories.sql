--liquibase formatted sql
--changeset zajonz:1

CREATE TABLE `users_subscribed_categories`
 (
     `users_id`                 INT NOT NULL,
     `subscribed_categories_id` INT NOT NULL,
     PRIMARY KEY (`users_id`, `subscribed_categories_id`),
     FOREIGN KEY (`users_id`) REFERENCES `users`(`id`),
     FOREIGN KEY (`subscribed_categories_id`) REFERENCES `category`(`id`)
);