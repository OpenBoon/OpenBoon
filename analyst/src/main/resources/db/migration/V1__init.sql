

CREATE TABLE pipeline (
        pk_pipeline UUID PRIMARY KEY NOT NULL,
        int_version BIGINT NOT NULL DEFAULT 1,
        json_processors TEXT NOT NULL,
        str_name TEXT NOT NULL,
        time_created BIGINT NOT NULL,
        time_modified BIGINT NOT NULL
);

CREATE UNIQUE INDEX pipeline_str_name_uniq_idx ON pipeline (str_name);

CREATE TABLE job (
        pk_job UUID PRIMARY KEY NOT NULL,
        pk_asset UUID NOT NULL,
        pk_organization UUID NOT NULL,
        int_state SMALLINT NOT NULL,
        str_name TEXT NOT NULL,
        time_created BIGINT NOT NULL,
        time_modified BIGINT NOT NULL,
        list_pipelines TEXT[] NOT NULL
);

CREATE INDEX job_pk_asset_idx ON job (pk_asset);
CREATE INDEX job_pk_organization_idx ON job (pk_organization);
CREATE INDEX job_int_state_idx ON job (int_state);

CREATE UNIQUE INDEX job_str_name_uniq_idx ON job (str_name);
