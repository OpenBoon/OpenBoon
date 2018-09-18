
CREATE TABLE analyst (
    pk_analyst UUID PRIMARY KEY,
    pk_task UUID,
    int_state SMALLINT DEFAULT 0 NOT NULL,
    int_lock_state SMALLINT DEFAULT 0 NOT NULL,
    time_created BIGINT NOT NULL,
    time_ping BIGINT NOT NULL,
    str_endpoint TEXT NOT NULL,
    int_total_ram INTEGER NOT NULL,
    int_free_ram INTEGER NOT NULL,
    flt_load FLOAT NOT NULL
);

CREATE INDEX analyst_pk_task_idx ON analyst(pk_task);
CREATE UNIQUE INDEX analyst_str_endpoint_idx ON analyst(str_endpoint);

---

ALTER TABLE pipeline ADD COLUMN time_created BIGINT NOT NULL DEFAULT 1536693258000;
ALTER TABLE pipeline ADD COLUMN time_modified BIGINT NOT NULL DEFAULT 1536693258000;

--


ALTER TABLE job ADD COLUMN int_priority SMALLINT DEFAULT 0 NOT NULL;
ALTER TABLE job ADD COLUMN time_created BIGINT NOT NULL DEFAULT 1536693258000;
ALTER TABLE job ADD COLUMN time_modified BIGINT NOT NULL DEFAULT 1536693258000;
ALTER TABLE job ADD COLUMN pk_user_modified UUID;
UPDATE job SET pk_user_modified='00000000-7B0B-480E-8C36-F06F04AED2F1';
ALTER TABLE job ALTER COLUMN pk_user_modified SET NOT NULL;
ALTER TABLE job DROP COLUMN str_root_path;

ALTER TABLE task DROP COLUMN pk_depend_parent;
ALTER TABLE task DROP COLUMN int_depend_count;
ALTER TABLE task ADD COLUMN time_modified BIGINT NOT NULL DEFAULT 1536693258000;
ALTER TABLE task ADD COLUMN json_script TEXT NOT NULL DEFAULT '{}';

---

CREATE TABLE task_error(
        pk_task_error UUID NOT NULL,
        pk_task UUID NOT NULL REFERENCES task(pk_task),
        pk_job UUID NOT NULL REFERENCES job(pk_job),
        pk_asset UUID NOT NULL,
        str_message TEXT,
        str_path TEXT,
        str_processor TEXT,
        str_endpoint TEXT,
        str_extension TEXT,
        time_created BIGINT NOT NULL,
        bool_fatal BOOLEAN NOT NULL
);

---

DROP TRIGGER trig_update_job_state ON job_count CASCADE;


---
ALTER TABLE job_count RENAME COLUMN int_task_state_waiting_count TO int_task_state_0;
ALTER TABLE job_count RENAME COLUMN int_task_state_running_count TO int_task_state_1;
ALTER TABLE job_count RENAME COLUMN int_task_state_success_count TO int_task_state_2;
ALTER TABLE job_count RENAME COLUMN int_task_state_failure_count TO int_task_state_3;
ALTER TABLE job_count RENAME COLUMN int_task_state_skipped_count TO int_task_state_4;
ALTER TABLE job_count RENAME COLUMN int_task_state_queued_count TO int_task_state_5;

CREATE OR REPLACE FUNCTION after_task_state_change() RETURNS TRIGGER AS $$
DECLARE
    old_state_col VARCHAR;
    new_state_col VARCHAR;
BEGIN
  old_state_col := 'int_task_state_' || old.int_state::text;
  new_state_col := 'int_task_state_' || new.int_state::text;
  EXECUTE 'UPDATE job_count SET ' || old_state_col || '=' || old_state_col || '-1,'
    || new_state_col || '=' || new_state_col || '+1 WHERE job_count.pk_job = $1' USING new.pk_job;
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trig_after_task_state_change AFTER UPDATE ON task
  FOR EACH ROW WHEN (OLD.int_state != NEW.int_state)
  EXECUTE PROCEDURE after_task_state_change();

---

CREATE OR REPLACE FUNCTION after_task_insert() RETURNS TRIGGER AS $$
BEGIN
  UPDATE job_count SET int_task_total_count=int_task_total_count+1,
    int_task_state_0=int_task_state_0+1 WHERE pk_job=new.pk_job;
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trig_after_task_insert AFTER INSERT ON task
  FOR EACH ROW EXECUTE PROCEDURE after_task_insert();
