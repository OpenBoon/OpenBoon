CREATE INDEX job_int_priority_idx ON job(int_priority);
CREATE INDEX task_time_created_idx ON task(time_created);

CREATE OR REPLACE FUNCTION after_task_state_change() RETURNS TRIGGER AS $$
DECLARE
  old_state_col VARCHAR;
  new_state_col VARCHAR;
  pending INTEGER;
BEGIN
  old_state_col := 'int_task_state_' || old.int_state::text;
  new_state_col := 'int_task_state_' || new.int_state::text;
  EXECUTE 'UPDATE job_count SET ' || old_state_col || '=' || old_state_col || '-1,'
    || new_state_col || '=' || new_state_col || '+1 WHERE job_count.pk_job = $1' USING new.pk_job;

  --- Check if the job is completed
  SELECT int_task_state_0 + int_task_state_1 + int_task_state_5 INTO pending FROM job_count WHERE pk_job=new.pk_job;
  IF pending = 0 THEN
      UPDATE job SET int_state=2 WHERE pk_job=new.pk_job AND int_state != 1;
  ELSE
      UPDATE job SET int_state=0 WHERE pk_job=new.pk_job AND int_state = 2;
  END IF;
  RETURN NEW;
END
$$ LANGUAGE plpgsql;
