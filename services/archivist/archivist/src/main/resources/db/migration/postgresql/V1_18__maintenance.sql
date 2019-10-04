--- Replace this trigger to set modified time.

CREATE OR REPLACE FUNCTION after_task_state_change() RETURNS TRIGGER AS $$
DECLARE
    old_state_col VARCHAR;
    new_state_col VARCHAR;
    tx_time BIGINT;
BEGIN
  SELECT EXTRACT(EPOCH FROM NOW()) * 1000 INTO tx_time;
  old_state_col := 'int_task_state_' || old.int_state::text;
  new_state_col := 'int_task_state_' || new.int_state::text;
  EXECUTE 'UPDATE job_count SET ' || old_state_col || '=' || old_state_col || '-1,'
    || new_state_col || '=' || new_state_col || '+1, time_updated=$1 WHERE job_count.pk_job = $2' USING tx_time, new.pk_job;
  RETURN NEW;
END
$$ LANGUAGE plpgsql;


CREATE TABLE cluster_lock (
  str_name TEXT PRIMARY KEY,
  str_host TEXT NOT NULL,
  time_locked BIGINT NOT NULL,
  time_expired BIGINT NOT NULL
);

