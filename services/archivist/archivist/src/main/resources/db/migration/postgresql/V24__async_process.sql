CREATE TABLE process
(
    pk_process     UUID PRIMARY KEY,
    pk_project     UUID NOT NULL,
    str_descr      TEXT NOT NULL,
    int_type       SMALLINT NOT NULL,
    int_state      SMALLINT NOT NULL,
    time_created   BIGINT NOT NULL,
    time_started   BIGINT NOT NULL,
    time_stopped   BIGINT NOT NULL,
    time_refresh   BIGINT NOT NULL,
    actor_created  TEXT NOT NULL
);

CREATE INDEX process_int_state_idx ON process (int_state);
CREATE INDEX process_time_created_idx ON process (time_created);
