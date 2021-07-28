ALTER TABLE model ADD COLUMN time_last_deployed BIGINT;
ALTER TABLE model ADD COLUMN actor_last_deployed TEXT;
ALTER TABLE model ADD COLUMN time_last_uploaded BIGINT;
ALTER TABLE model ADD COLUMN actor_last_uploaded TEXT;

ALTER TABLE model ADD COLUMN int_state SMALLINT NOT NULL DEFAULT 2;
ALTER TABLE model ADD COLUMN json_depends JSONB NOT NULL DEFAULT '[]';
