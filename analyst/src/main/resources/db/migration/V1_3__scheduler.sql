
---
--- Add a type for the job, which will be something like import, export, etc
---
ALTER TABLE job ADD COLUMN int_type SMALLINT NOT NULL DEFAULT 0;

---
--- Create a table to ensure we're only running 1 ingest per asset.
---
CREATE TABLE lock (
  pk_lock UUID PRIMARY KEY NOT NULL,
  pk_job UUID NOT NULL,
  pk_asset UUID NOT NULL,
  time_created BIGINT NOT NULL
);

CREATE UNIQUE INDEX lock_pk_asset_idx ON lock (pk_asset);
