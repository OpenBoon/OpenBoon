
DROP TABLE IF EXISTS person;
CREATE TABLE person(
  pk_person INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_username VARCHAR(255) NOT NULL,
  str_password VARCHAR(255) NOT NULL,
  str_email VARCHAR(255) NOT NULL,
  list_roles ARRAY NOT NULL
);

CREATE UNIQUE INDEX person_str_username_idx ON person(str_username);

DROP TABLE IF EXISTS room;
CREATE TABLE room(
  pk_room BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_name VARCHAR(255) NOT NULL,
  str_session VARCHAR(32),
  str_password VARCHAR(255),
  bool_visible BOOLEAN NOT NULL DEFAULT 't',
  list_invites ARRAY
);
