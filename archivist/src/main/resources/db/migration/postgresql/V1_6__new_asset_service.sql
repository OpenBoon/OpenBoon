

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
  bool_metadata_stored BOOLEAN NOT NULL DEFAULT 'f',
  bool_file_stored BOOLEAN NOT NULL DEFAULT 'f',
  time_created BIGINT NOT NULL,
  time_modified BIGINT NOT NULL
);

CREATE INDEX asset_pk_organization_idx ON asset (pk_organization);
CREATE INDEX asset_ready_state_idx ON asset (bool_metadata_stored, bool_file_stored);

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

CREATE INDEX task_pk_organization_idx ON task (pk_organization);
CREATE INDEX task_pk_asset_idx ON task (pk_asset);
CREATE INDEX task_pk_pipeline_idx ON task (pk_pipeline);

