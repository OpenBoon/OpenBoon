
CREATE TABLE command(
  pk_command INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_user INT NOT NULL,
  time_created BIGINT NOT NULL,
  time_started BIGINT NOT NULL DEFAULT -1,
  time_stopped BIGINT NOT NULL DEFAULT -1,
  int_state TINYINT NOT NULL DEFAULT 0,
  int_type TINYINT NOT NULL,
  str_message VARCHAR(255),
  int_total_count BIGINT NOT NULL DEFAULT 0,
  int_success_count BIGINT NOT NULL DEFAULT 0,
  int_error_count BIGINT NOT NULL DEFAULT 0,
  json_args TEXT NOT NULL
);




