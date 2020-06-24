--
-- Name: project int_tier; Type: COLUMN; Schema: zorroa; Owner: zorroa
--
CREATE TABLE depend(
    pk_depend UUID PRIMARY KEY,
    int_type SMALLINT NOT NULL,
    int_state SMALLINT NOT NULL DEFAULT 1,
    pk_job_depend_er UUID NOT NULL REFERENCES job(pk_job),
    pk_job_depend_on UUID  NOT NULL REFERENCES job(pk_job),
    pk_task_depend_er UUID REFERENCES task(pk_task),
    pk_task_depend_on UUID  REFERENCES task(pk_task),
    time_created BIGINT NOT NULL,
    time_modified BIGINT NOT NULL,
    actor_created TEXT NOT NULL,
    actor_modified TEXT NOT NULL
);

CREATE INDEX depend_pk_job_depend_er_idx ON depend (pk_job_depend_er);
CREATE INDEX depend_pk_job_depend_on_idx ON depend (pk_job_depend_on);
CREATE INDEX depend_pk_task_depend_er_idx ON depend (pk_task_depend_er);
CREATE INDEX depend_pk_task_depend_on_idx ON depend (pk_task_depend_er);
CREATE INDEX depend_int_state_idx ON depend (int_state);

ALTER TABLE job DROP COLUMN int_type;

ALTER TABLE task ADD COLUMN int_depend_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE job_count ADD COLUMN int_task_state_6 integer DEFAULT 0 NOT NULL;


CREATE FUNCTION set_task_to_waiting() RETURNS TRIGGER
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.int_state = 0;
    RETURN NEW;
END
$$;

CREATE FUNCTION set_task_to_depend() RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.int_state = 6;
    RETURN NEW;
END
$$;

CREATE TRIGGER task_flip_to_waiting BEFORE UPDATE ON task
    FOR EACH ROW WHEN (NEW.int_depend_count = 0 AND NEW.int_state=6)
    EXECUTE PROCEDURE set_task_to_waiting();


CREATE TRIGGER task_flip_to_depend BEFORE UPDATE ON task
    FOR EACH ROW WHEN (NEW.int_depend_count > 0 AND NEW.int_state=0)
    EXECUTE PROCEDURE set_task_to_depend();
