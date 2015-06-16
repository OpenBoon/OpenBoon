
DROP TABLE IF EXISTS user;
CREATE TABLE user(
  pk_user INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_username VARCHAR(255) NOT NULL,
  str_password VARCHAR(100) NOT NULL,
  str_email VARCHAR(255) NOT NULL,
  list_roles ARRAY NOT NULL
);

CREATE UNIQUE INDEX user_str_username_idx ON user(str_username);

DROP TABLE IF EXISTS room;
CREATE TABLE room(
  pk_room BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_name VARCHAR(255) NOT NULL,
  str_session VARCHAR(32),
  str_password VARCHAR(100),
  bool_visible BOOLEAN NOT NULL DEFAULT 't',
  list_invites ARRAY
);

DROP TABLE IF EXISTS proxy_config;
CREATE TABLE proxy_config (
  pk_proxy_config INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_name VARCHAR(64) NOT NULL,
  str_description VARCHAR(255) NOT NULL,
  str_user_created VARCHAR(128) NOT NULL,
  time_created BIGINT NOT NULL,
  str_user_modified VARCHAR(128) NOT NULL,
  time_modified BIGINT NOT NULL

);

CREATE TABLE proxy_config_entry (
  pk_proxy_config_entry INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_proxy_config INT NOT NULL,
  str_format VARCHAR(255) NOT NULL,
  int_size INT NOT NULL,
  int_bpp INT NOT NULL,
  FOREIGN KEY (pk_proxy_config) REFERENCES proxy_config (pk_proxy_config) ON DELETE CASCADE
);

CREATE TABLE pipeline (
  pk_pipeline INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_name VARCHAR(64) NOT NULL,
  str_description VARCHAR(255) NOT NULL,
  str_user_created VARCHAR(128) NOT NULL,
  time_created BIGINT NOT NULL,
  str_user_modified VARCHAR(128) NOT NULL,
  time_modified BIGINT NOT NULL,
  list_processors OTHER NOT NULL
);

