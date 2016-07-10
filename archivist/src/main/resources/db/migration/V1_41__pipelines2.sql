DROP TABLE ingest_skip;
DROP TABLE ingest;
DROP TABLE schedule;
DROP TABLE pipeline;

CREATE TABLE pipeline (
  pk_pipeline INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  int_type SMALLINT NOT NULL,
  str_name VARCHAR(255) NOT NULL,
  json_processors TEXT NOT NULL
);

CREATE UNIQUE INDEX pipeline_name_uniq_idx ON pipeline (str_name);

CREATE TABLE pipeline_acl (
  pk_permission INTEGER NOT NULL,
  pk_pipeline INTEGER NOT NULL,
  int_access INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY(pk_pipeline, pk_permission)
);

CREATE INDEX pipeline_acl_permission_idx ON pipeline_acl (pk_permission);

CREATE TABLE macro (
  pk_macro  INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  int_type SMALLINT NOT NULL,
  str_name VARCHAR(255) NOT NULL,
  str_desc TEXT NOT NULL DEFAULT '',
  json_processors TEXT NOT NULL DEFAULT 0
);

CREATE TABLE macro_acl (
  pk_permission INTEGER NOT NULL,
  pk_macro INTEGER NOT NULL,
  int_access INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY(pk_macro, pk_permission)
);

CREATE INDEX macro_acl_permission_idx ON macro_acl (pk_permission);

---
--- Ingests create imports
---
CREATE TABLE ingest (
  pk_ingest INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
  pk_folder INTEGER REFERENCES folder ON DELETE SET NULL,
  --- The pipeline can be null and the ingest could just
  --- have a custom script.  If the pipeline is deleted
  --- from the system, the ingest becomes disabled.
  pk_pipeline INTEGER REFERENCES pipeline ON DELETE SET NULL,
  str_name VARCHAR(128) NOT NULL,
  time_executed BIGINT NOT NULL DEFAULT -1,
  time_created BIGINT NOT NULL DEFAULT -1,
  int_user_created INTEGER NOT NULL,
  crond_trigger VARCHAR(128) NOT NULL,
  bool_automatic BOOLEAN NOT NULL DEFAULT 0,
  json_generators TEXT NOT NULL,
  json_pipeline TEXT
);

CREATE UNIQUE INDEX ingest_name_uniq_idx ON ingest (str_name);
CREATE UNIQUE INDEX ingest_folder_uniq_idx ON ingest (pk_folder);
CREATE INDEX ingest_pipeline_idx ON ingest (pk_pipeline);



