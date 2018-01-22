

CREATE TABLE request (
  pk_request INTEGER PRIMARY KEY AUTO_INCREMENT,
  user_created INTEGER NOT NULL,
  time_created BIGINT NOT NULL,
  json_search VARCHAR(52428) NOT NULL,
  int_count INTEGER NOT NULL,
  int_state SMALLINT NOT NULL DEFAULT 0,
  str_comment VARCHAR(1024) NOT NULL
);

CREATE INDEX request_int_state_idx ON request(int_state);


