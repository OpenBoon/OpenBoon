
DROP TABLE request;
CREATE TABLE request (
  pk_request INTEGER PRIMARY KEY AUTO_INCREMENT,
  pk_folder  INTEGER REFERENCES folder(pk_folder),
  user_created INTEGER NOT NULL REFERENCES users(pk_user),
  user_modified INTEGER NOT NULL REFERENCES users(pk_user),
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  int_count INTEGER NOT NULL,
  int_state SMALLINT NOT NULL DEFAULT 0,
  int_type SMALLINT NOT NULL DEFAULT 0,
  str_comment VARCHAR(1024) NOT NULL,
  json_cc VARCHAR(2048) NOT NULL
);

CREATE INDEX request_int_state_idx ON request(int_state);


