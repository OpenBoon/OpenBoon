

CREATE TABLE schedule (
  pk_schedule INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
  str_name VARCHAR(255) NOT NULL,
  str_user_created VARCHAR(128) NOT NULL,
  time_created BIGINT NOT NULL,
  str_user_modified VARCHAR(128) NOT NULL,
  time_modified BIGINT NOT NULL,
  time_executed BIGINT NOT NULL DEFAULT 0,
  time_next BIGINT NOT NULL DEFAULT -1,
  clock_run_at_time TIME NOT NULL DEFAULT '00:00:00',
  csv_days VARCHAR(16) NOT NULL,
  bool_enabled BOOLEAN DEFAULT 1
);

---
--- Use a mapping table to store our schedule to ingest mapping
--- so it can be automatically cleaned up if an ingest is deleted.
---
CREATE TABLE map_schedule_to_ingest (
  pk_ingest BIGINT NOT NULL REFERENCES ingest(pk_ingest) ON DELETE CASCADE,
  pk_schedule INT NOT NULL REFERENCES schedule(pk_schedule) ON DELETE CASCADE,
  PRIMARY KEY (pk_schedule, pk_ingest)
);

CREATE INDEX map_schedule_to_ingest_pk_ingest_idx ON map_schedule_to_ingest(pk_ingest);


