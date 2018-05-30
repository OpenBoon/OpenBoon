

DROP TABLE job_count;
DROP TABLE job_stat;
DROP TABLE task_stat;
DROP TABLE task;
DROP TABLE job;

CREATE TABLE asset (
  pk_asset UUID PRIMARY KEY,
  pk_organization UUID NOT NULL,
  pk_user_created UUID NOT NULL,
  pk_user_modified UUID NOT NULL,
  str_filename TEXT NOT NULL,
  str_location TEXT,
  int_state SMALLINT NOT NULL DEFAULT 0,
  bool_direct BOOLEAN NOT NULL,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  int_task_count INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX asset_pk_organization_idx ON asset (pk_organization);

CREATE TABLE task (
  pk_task UUID PRIMARY KEY,
  pk_asset UUID NOT NULL,
  pk_organization UUID NOT NULL,
  pk_pipeline UUID,
  int_version BIGINT NOT NULL,
  str_name TEXT NOT NULL,
  int_state SMALLINT NOT NULL DEFAULT 0,
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL,
  pk_user_created UUID NOT NULL,
  pk_user_modified UUID NOT NULL,
  int_order BIGINT NOT NULL
);




