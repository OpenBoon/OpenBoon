
DROP TABLE IF EXISTS person;
CREATE TABLE person(
  pk_person INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_username VARCHAR(255) NOT NULL,
  str_password VARCHAR(1024) NOT NULL,
  str_email VARCHAR(255) NOT NULL,
  list_roles ARRAY NOT NULL
);

CREATE UNIQUE INDEX person_str_username_idx ON person(str_username);