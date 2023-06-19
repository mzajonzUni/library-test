--liquibase formatted sql
--changeset zajonz:1

INSERT INTO category(`name`) VALUES ('Dramat');
INSERT INTO category(`name`) VALUES ('Thriller');
INSERT INTO category(`name`) VALUES ('Romans');
INSERT INTO category(`name`) VALUES ('Epika');
INSERT INTO users(`firstname`,`lastname`,`username`,`password`,`email`,`role`,`locked`) VALUES ('Adam','Adam','Adam','$2a$10$444eH1zIsXWUzH7sSQCAgeb8sa6Hfx8uzu1GxtZ81.AFtARrcR.rC','zajonz.mateusz@gmail.com','ROLE_CUSTOMER',0);
INSERT INTO users(`firstname`,`lastname`,`username`,`password`,`email`,`role`,`locked`) VALUES ('asd','asd','asd','$2a$10$444eH1zIsXWUzH7sSQCAgeb8sa6Hfx8uzu1GxtZ81.AFtARrcR.rC','zajonzm.ateusz@gmail.com','ROLE_EMPLOYEE',0);
INSERT INTO users(`firstname`,`lastname`,`username`,`password`,`email`,`role`,`locked`) VALUES ('Marek','Marek','Marek','$2a$10$444eH1zIsXWUzH7sSQCAgeb8sa6Hfx8uzu1GxtZ81.AFtARrcR.rC','zajonzma.teusz@gmail.com','ROLE_CUSTOMER',0);
INSERT INTO users(`firstname`,`lastname`,`username`,`password`,`email`,`role`,`locked`) VALUES ('Mateusz','Mateusz','Mateusz','$2a$10$444eH1zIsXWUzH7sSQCAgeb8sa6Hfx8uzu1GxtZ81.AFtARrcR.rC','zajonzmat.eusz@gmail.com','ROLE_CUSTOMER',0);
INSERT INTO book(`author`,`state`,`title`,`is_blocked`,`category_id`) VALUES ('Adam Mickiewicz',0,'Dziady',1,1);
INSERT INTO book(`author`,`state`,`title`,`is_blocked`,`category_id`) VALUES ('Adam Mickiewicz',0,'Pan Tadeusz',0,2);
INSERT INTO book(`author`,`state`,`title`,`is_blocked`,`category_id`) VALUES ('Adam Mickiewicz',0,'Burza',0,3);
INSERT INTO book(`author`,`state`,`title`,`is_blocked`,`category_id`) VALUES ('Henryk Sienkiewicz',0,'Potop',0,4);
INSERT INTO book(`author`,`state`,`title`,`is_blocked`,`category_id`) VALUES ('Henryk Sienkiewicz',0,'Krzyżacy',0,1);
INSERT INTO book(`author`,`state`,`title`,`is_blocked`,`category_id`) VALUES ('Henryk Sienkiewicz',0,'Bajka',0,2);
INSERT INTO users_subscribed_categories(`users_id`,`subscribed_categories_id`) VALUES (1,1);
INSERT INTO users_subscribed_categories(`users_id`,`subscribed_categories_id`) VALUES (3,1);

