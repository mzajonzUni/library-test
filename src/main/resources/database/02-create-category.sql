--liquibase formatted sql
--changeset zajonz:1

CREATE TABLE `category`
(
    `id`   INT         NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`id`)
);