--liquibase formatted sql
--changeset zajonz:1

INSERT INTO users(`firstname`,`lastname`,`username`,`password`,`email`,`role`,`locked`) VALUES ('TestFirstE','TestLastE','TestUserE','$2a$10$iJQ9lukfHn.dWMcuumYy5Or8tqOWdpXYGxuUUwKgjK9XMD99WjzKa','zajonz.mateusz@gmail.com','ROLE_EMPLOYEE',0);
INSERT INTO users(`firstname`,`lastname`,`username`,`password`,`email`,`role`,`locked`) VALUES ('TestFirstC','TestLastC','TestLastC','$2a$10$iJQ9lukfHn.dWMcuumYy5Or8tqOWdpXYGxuUUwKgjK9XMD99WjzKa','zajonzm.ateusz@gmail.com','ROLE_CUSTOMER',0);


