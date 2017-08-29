
DROP TABLE job_count;
CREATE TABLE job_count (
  pk_job INTEGER PRIMARY KEY NOT NULL,
  int_task_total_count INTEGER NOT NULL DEFAULT 0,
  int_task_completed_count INTEGER NOT NULL DEFAULT 0,
  int_task_state_waiting_count INTEGER NOT NULL DEFAULT 0,
  int_task_state_queued_count INTEGER NOT NULL DEFAULT 0,
  int_task_state_running_count INTEGER NOT NULL DEFAULT 0,
  int_task_state_success_count INTEGER NOT NULL DEFAULT 0,
  int_task_state_failure_count INTEGER NOT NULL DEFAULT 0
);

DROP TABLE job_stat;
CREATE TABLE job_stat (
  pk_job INTEGER PRIMARY KEY NOT NULL,
  int_frame_total_count INTEGER NOT NULL DEFAULT 0,
  int_frame_success_count INTEGER NOT NULL DEFAULT 0,
  int_frame_error_count INTEGER NOT NULL DEFAULT 0,
  int_frame_warning_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE task_stat (
  pk_task INTEGER PRIMARY KEY NOT NULL,
  pk_job INTEGER NOT NULL,
  int_frame_total_count INTEGER NOT NULL DEFAULT 0,
  int_frame_success_count INTEGER NOT NULL DEFAULT 0,
  int_frame_error_count INTEGER NOT NULL DEFAULT 0,
  int_frame_warning_count INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX task_stat_pk_job_idx ON task_stat (pk_job);

