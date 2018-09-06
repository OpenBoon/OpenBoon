
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