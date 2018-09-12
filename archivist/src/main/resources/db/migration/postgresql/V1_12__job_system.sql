
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


CREATE TABLE job (
        pk_job UUID PRIMARY KEY NOT NULL,
        pk_organization UUID NOT NULL,
        int_state SMALLINT DEFAULT 0 NOT NULL,
        int_type SMALLINT NOT NULL,
        int_priority SMALLINT DEFAULT 0 NOT NULL,
        str_name TEXT NOT NULL,
        time_created BIGINT NOT NULL,
        time_modified BIGINT NOT NULL,
        user_created UUID NOT NULL,
        user_modified UUID NOT NULL,
        json_args TEXT NOT NULL DEFAULT '{}',
        json_env TEXT NOT NULL DEFAULT '{}'
);

CREATE INDEX job_pk_organization_idx ON job (pk_organization);
CREATE INDEX job_int_state_idx ON job (int_state);
CREATE INDEX job_int_type_idx ON job (int_type);

CREATE TABLE task(
        pk_task UUID PRIMARY KEY,
        pk_job UUID NOT NULL,
        str_name TEXT NOT NULL,
        int_state SMALLINT DEFAULT 0 NOT NULL,
        time_started BIGINT DEFAULT -1 NOT NULL,
        time_stopped BIGINT DEFAULT -1 NOT NULL,
        time_created BIGINT NOT NULL,
        time_modified BIGINT NOT NULL,
        time_ping BIGINT DEFAULT 0 NOT NULL,
        int_priority SMALLINT DEFAULT 0 NOT NULL,
        int_exit_status SMALLINT DEFAULT -1 NOT NULL,
        str_endpoint TEXT,
        json_script JSONB NOT NULL
);


CREATE INDEX task_pk_job_idx ON task(pk_job);
CREATE INDEX task_int_state_idx ON task(int_state);
CREATE INDEX task_int_priority_idx ON task(int_priority);
CREATE INDEX task_str_namne ON task(str_name);

CREATE TABLE task_error(
        pk_task_error UUID NOT NULL,
        pk_task UUID NOT NULL,
        pk_job UUID NOT NULL,
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

ALTER TABLE export_file ADD COLUMN pk_organization UUID;
UPDATE export_file SET pk_organization='00000000-9998-8888-7777-666666666666';
ALTER TABLE export_file ALTER COLUMN pk_organization SET NOT NULL;

