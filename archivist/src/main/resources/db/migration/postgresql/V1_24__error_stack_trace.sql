
CREATE INDEX IF NOT EXISTS job_time_created_idx ON job (time_created);

ALTER TABLE task_error ADD COLUMN json_stack_trace JSONB;