CREATE TABLE automl(
    pk_automl UUID PRIMARY KEY,
    pk_model UUID NOT NULL REFERENCES model(pk_model),
    pk_project UUID NOT NULL REFERENCES project(pk_project),
    str_dataset TEXT NOT NULL,
    str_training_job TEXT NOT NULL,
    str_model TEXT,
    str_error TEXT,
    int_state SMALLINT NOT NULL DEFAULT 0,
    time_created BIGINT NOT NULL,
    time_modified BIGINT NOT NULL,
    actor_created TEXT NOT NULL,
    actor_modified TEXT NOT NULL
);

CREATE INDEX automl_pk_model_idx ON automl (pk_model);
CREATE INDEX automl_pk_project_idx ON automl (pk_project);
CREATE INDEX automl_int_state_time_modified_idx ON automl (int_state, time_modified);
