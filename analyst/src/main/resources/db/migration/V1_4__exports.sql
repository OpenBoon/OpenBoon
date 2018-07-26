
ALTER TABLE pipeline ADD COLUMN int_type SMALLINT NOT NULL DEFAULT 0;

ALTER TABLE job DROP COLUMN pk_asset;
ALTER TABLE job DROP COLUMN list_pipelines;
ALTER TABLE job ADD COLUMN bool_lock_assets BOOLEAN NOT NULL DEFAULT 'f';
ALTER TABLE job ADD COLUMN time_started BIGINT NOT NULL DEFAULT -1;
ALTER TABLE job ADD COLUMN time_stopped BIGINT NOT NULL DEFAULT -1;

CREATE TABLE x_asset_job (
  pk_x_asset_job UUID PRIMARY KEY NOT NULL,
  pk_job UUID NOT NULL REFERENCES job (pk_job) ON DELETE CASCADE,
  pk_asset UUID NOT NULL
);

CREATE INDEX x_asset_job_pk_job_idx ON x_asset_job (pk_job);
CREATE INDEX x_asset_job_pk_asset_idx ON x_asset_job (pk_asset);
