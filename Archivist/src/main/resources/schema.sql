

DROP TABLE IF EXISTS user;
CREATE TABLE user(
  pk_user INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_username VARCHAR(255) NOT NULL,
  str_password VARCHAR(100) NOT NULL,
  str_email VARCHAR(255) NOT NULL,
  list_roles ARRAY NOT NULL,
  bool_enabled BOOLEAN NOT NULL
);

CREATE UNIQUE INDEX user_str_username_idx ON user(str_username);

DROP TABLE IF EXISTS session;
CREATE TABLE session (
  pk_session BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_user INT NOT NULL REFERENCES user (pk_user),
  cookie_id VARCHAR(128) NOT NULL,
  bool_expired BOOLEAN NOT NULL DEFAULT 'f',
  time_last_request BIGINT NOT NULL
);

CREATE UNIQUE INDEX session_cookie_id_uniq_idx ON session (cookie_id);
CREATE INDEX session_pk_user_idx ON session(pk_user);


DROP TABLE IF EXISTS room;
CREATE TABLE room (
  pk_room BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  --- The session is set if room is tied to a session, otherwise is a permanent room.
  pk_session BIGINT REFERENCES session(pk_session) ON DELETE CASCADE,
  str_name VARCHAR(255) NOT NULL,
  str_password VARCHAR(100),
  bool_visible BOOLEAN NOT NULL DEFAULT 't',
  list_invites ARRAY
);

CREATE TABLE map_session_to_room (
  id BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_session BIGINT NOT NULL REFERENCES session (pk_session) ON DELETE CASCADE,
  --- The room, may be null
  pk_room BIGINT REFERENCES room (pk_room) ON DELETE SET NULL
);

--- The session can only be in this table once, the room however may change.
CREATE UNIQUE INDEX map_session_to_room_uniq_idx ON map_session_to_room (pk_session);


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

CREATE UNIQUE INDEX proxy_config_str_name_uniq_idx ON proxy_config(str_name);


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

CREATE UNIQUE INDEX pipeline_str_name_uniq_idx ON pipeline(str_name);

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
  int_error_count INT NOT NULL DEFAULT 0,
  bool_update_on_exist BOOLEAN NOT NULL
);
