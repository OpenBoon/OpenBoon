CREATE TABLE model
(
    pk_model        uuid PRIMARY KEY,
    pk_project      uuid NOT NULL REFERENCES project (pk_project) ON DELETE CASCADE,
    pk_data_set     uuid NOT NULL REFERENCES data_set (pk_data_set) ON DELETE CASCADE,
    str_name        text     NOT NULL,
    int_type        smallint NOT NULL,
    str_file_id     text     NOT NULL,
    str_job_name    text     NOT NULL,
    bool_trained    boolean  NOT NULL DEFAULT 'f',
    int_version     integer  NOT NULL DEFAULT 1,
    time_created    bigint   NOT NULL,
    time_modified   bigint   NOT NULL,
    time_trained    bigint   NOT NULL DEFAULT -1,
    actor_created   text     NOT NULL,
    actor_modified  text     NOT NULL
);

CREATE UNIQUE INDEX model_pk_dataset_uniq_idx ON model (pk_data_set, int_type);
CREATE INDEX model_pk_project_idx ON model(pk_project);
