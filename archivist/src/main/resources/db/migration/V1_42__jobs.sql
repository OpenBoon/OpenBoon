
CREATE TABLE job (
  pk_job INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_name VARCHAR(255) NOT NULL,
  int_type TINYINT NOT NULL DEFAULT 0,
  int_state TINYINT NOT NULL DEFAULT 0,
  int_user_created INTEGER NOT NULL,
  time_started BIGINT NOT NULL,
  time_stopped BIGINT NOT NULL DEFAULT -1,
  json_script TEXT NOT NULL
);

CREATE INDEX job_name_idx ON job (str_name);
CREATE INDEX job_type_idx ON job (int_type);
CREATE INDEX job_state_idx ON job (int_state);

CREATE TABLE job_count (
  pk_job INTEGER NOT NULL REFERENCES job ON DELETE CASCADE,
  int_task_total_count INTEGER NOT NULL DEFAULT 0,
  int_task_completed_count INTEGER NOT NULL DEFAULT 0,
  int_task_state_waiting_count INTEGER NOT NULL DEFAULT 0,
  int_task_state_queued_count INTEGER NOT NULL DEFAULT 0,
  int_task_state_running_count INTEGER NOT NULL DEFAULT 0,
  int_task_state_success_count INTEGER NOT NULL DEFAULT 0,
  int_task_state_failure_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE job_stat (
  pk_job INTEGER NOT NULL REFERENCES job ON DELETE CASCADE,
  int_asset_total_count INTEGER NOT NULL DEFAULT 0,
  int_asset_create_count INTEGER NOT NULL DEFAULT 0,
  int_asset_update_count INTEGER NOT NULL DEFAULT 0,
  int_asset_error_count INTEGER NOT NULL DEFAULT 0,
  int_asset_warning_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE task (
  pk_task INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_parent INTEGER REFERENCES task ON DELETE SET NULL,
  pk_job INTEGER NOT NULL REFERENCES job (pk_job) ON DELETE CASCADE,
  int_state TINYINT NOT NULL DEFAULT 0,
  time_started BIGINT NOT NULL DEFAULT -1,
  time_stopped BIGINT NOT NULL DEFAULT -1,
  json_script TEXT NOT NULL,
  int_order SMALLINT NOT NULL DEFAULT 0,
  str_execute VARCHAR(16) NOT NULL,
  int_exit_status SMALLINT NOT NULL DEFAULT -1,
  str_host VARCHAR(255)
);

CREATE INDEX task_parent_idx ON task (pk_parent);
CREATE INDEX task_pk_job_idx ON task (pk_job);
CREATE INDEX task_state_idx ON task (int_state);
CREATE INDEX task_order_idx ON task (int_order);
