
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
  time_modified BIGINT NOT NULL,
  list_outputs OTHER NOT NULL
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

CREATE TABLE ingest (
  pk_ingest BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_pipeline INT NOT NULL,
  pk_proxy_config INT NOT NULL,
  int_state TINYINT NOT NULL DEFAULT 0,
  str_path VARCHAR(1024) NOT NULL,
  list_types OTHER NOT NULL,
  str_user_created VARCHAR(128) NOT NULL,
  time_created BIGINT NOT NULL,
  str_user_modified VARCHAR(128) NOT NULL,
  time_modified BIGINT NOT NULL,
  time_started BIGINT NOT NULL DEFAULT -1,
  time_stopped BIGINT NOT NULL DEFAULT -1,
  int_created_count INT NOT NULL DEFAULT 0,
  int_error_count INT NOT NULL DEFAULT 0
);
