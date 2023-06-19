--liquibase formatted sql
--changeset zajonz:1

CREATE TABLE `users`
(
    `id`        INT          NOT NULL AUTO_INCREMENT,
    `firstname` VARCHAR(50)  NOT NULL,
    `lastname`  VARCHAR(50)  NOT NULL,
    `email`     VARCHAR(100) NOT NULL,
    `username`  VARCHAR(50)  NOT NULL,
    `password`  VARCHAR(500)  NOT NULL,
    `role`      VARCHAR(15)  NOT NULL,
    `locked`    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

ALTER TABLE  `users` ADD CONSTRAINT username_UNIQUE UNIQUE(`username`);
ALTER TABLE  `users` ADD CONSTRAINT email_UNIQUE UNIQUE(`email`);