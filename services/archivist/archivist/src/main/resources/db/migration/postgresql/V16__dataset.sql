CREATE TABLE dataset
(
    pk_dataset     UUID PRIMARY KEY,
    pk_project     UUID   NOT NULL REFERENCES project (pk_project) ON DELETE CASCADE,
    str_name       TEXT   NOT NULL,
    int_type       SMALLINT NOT NULL,
    time_created   BIGINT NOT NULL,
    time_modified  BIGINT NOT NULL,
    actor_created  TEXT   NOT NULL,
    actor_modified TEXT   NOT NULL
);

CREATE INDEX dataset_pk_project_idx ON dataset (pk_project);
CREATE UNIQUE INDEX dataset_str_name_pk_project_idx ON dataset (str_name, pk_project);

INSERT INTO dataset (pk_dataset, pk_project, str_name, int_type, time_created, time_modified, actor_created,
                     actor_modified)
SELECT pk_model,
       pk_project,
       str_name,
       int_type,
       time_created,
       time_modified,
       actor_created,
       actor_modified
FROM model;
