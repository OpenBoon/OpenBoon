
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

CREATE TABLE pipeline (
        pk_pipeline UUID PRIMARY KEY NOT NULL,
        str_name TEXT NOT NULL,
        int_type SMALLINT NOT NULL,
        json_processors TEXT NOT NULL,
        time_created BIGINT NOT NULL,
        time_modified BIGINT NOT NULL
);

CREATE UNIQUE INDEX pipeline_str_name_uniq_idx ON pipeline (str_name);

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