
CREATE TABLE data_set
(
    pk_data_set uuid PRIMARY KEY,
    pk_project       uuid REFERENCES project (pk_project),
    str_name         text,
    int_type         smallint,
    time_created     bigint NOT NULL,
    time_modified    bigint NOT NULL,
    actor_created    text   NOT NULL,
    actor_modified   text   NOT NULL
);

CREATE UNIQUE INDEX data_set_uniq_idx ON data_set (pk_project, str_name);