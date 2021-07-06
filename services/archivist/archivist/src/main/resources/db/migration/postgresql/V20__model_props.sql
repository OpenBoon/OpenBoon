ALTER TABLE model ADD COLUMN time_last_trained BIGINT;
ALTER TABLE model ADD COLUMN actor_last_trained TEXT;
ALTER TABLE model ADD COLUMN time_last_applied BIGINT;
ALTER TABLE model ADD COLUMN actor_last_applied TEXT;
ALTER TABLE model ADD COLUMN time_last_tested BIGINT;
ALTER TABLE model ADD COLUMN actor_last_tested TEXT;
