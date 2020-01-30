
CREATE TABLE credentials (
    pk_credentials      uuid PRIMARY KEY,
    pk_project          uuid NOT NULL REFERENCES project(pk_project) ON DELETE CASCADE,
    str_name            varchar(64) NOT NULL,
    int_type            smallint NOT NULL,
    str_blob            text,
    time_created        bigint NOT NULL,
    time_modified       bigint NOT NULL,
    actor_created       text NOT NULL,
    actor_modified      text NOT NULL
);

CREATE UNIQUE INDEX credentials_pk_project_str_name_uniq_idx ON credentials (pk_project, str_name);

CREATE TABLE x_credentials_datasource
(
    pk_x_credentials_datasource uuid PRIMARY KEY,
    pk_credentials              uuid NOT NULL REFERENCES credentials (pk_credentials) ON DELETE CASCADE,
    pk_datasource               uuid NOT NULL REFERENCES datasource (pk_datasource) ON DELETE CASCADE,
    int_type                    smallint NOT NULL
);

CREATE UNIQUE INDEX x_credentials_datasource_uniq_idx ON x_credentials_datasource (pk_datasource, int_type);
CREATE INDEX x_credentials_datasource_pk_credentials_idx ON x_credentials_datasource (pk_credentials);

CREATE TABLE x_credentials_job
(
    pk_x_credentials_job        uuid PRIMARY KEY,
    pk_credentials              uuid NOT NULL REFERENCES credentials (pk_credentials) ON DELETE CASCADE,
    pk_job                      uuid NOT NULL REFERENCES job (pk_job) ON DELETE CASCADE,
    int_type                    smallint NOT NULL
);

CREATE UNIQUE INDEX x_credentials_job_uniq_idx ON x_credentials_job (pk_job, int_type);
CREATE INDEX x_credentials_job_pk_credentials_idx ON x_credentials_job (pk_credentials);
