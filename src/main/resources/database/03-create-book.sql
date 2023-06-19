--liquibase formatted sql
--changeset zajonz:1

CREATE TABLE `book`
(
    `id`          INT         NOT NULL AUTO_INCREMENT,
    `title`       VARCHAR(60) NOT NULL,
    `author`      VARCHAR(50) NOT NULL,
    `is_blocked`  TINYINT     NOT NULL DEFAULT 0,
    `state`       INT         NOT NULL,
    `from_date`   DATE        NULL,
    `to_date`     DATE        NULL,
    `category_id` INT         NOT NULL,
    `user_id`     INT         NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`category_id`) REFERENCES `category`(`id`)
);

