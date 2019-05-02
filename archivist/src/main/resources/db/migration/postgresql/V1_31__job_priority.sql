
CREATE INDEX IF NOT EXISTS job_pk_organization_idx ON job (pk_organization);
CREATE INDEX job_count_int_task_state_0_idx ON job_count (int_task_state_0);