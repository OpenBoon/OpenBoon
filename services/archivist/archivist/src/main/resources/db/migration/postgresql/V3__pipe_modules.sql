
ALTER TABLE pipeline RENAME COLUMN int_slot TO int_mode;
ALTER TABLE pipeline ADD COLUMN actor_created text NOT NULL;
ALTER TABLE pipeline ADD COLUMN actor_modified text NOT NULL;
ALTER TABLE pipeline ADD COLUMN _json_processors jsonb NOT NULL DEFAULT '[]'::jsonb;
UPDATE pipeline SET _json_processors=json_processors::jsonb;
ALTER TABLE pipeline DROP COLUMN json_processors;
ALTER TABLE pipeline RENAME COLUMN _json_processors TO  json_processors;


CREATE TABLE module
(
    pk_module       uuid PRIMARY KEY,
    str_name        text                      NOT NULL,
    int_version     integer                   NOT NULL DEFAULT 1,
    str_description text                      NOT NULL,
    json_ops        jsonb DEFAULT '{}'::jsonb NOT NULL,
    bool_restricted boolean                   NOT NULL DEFAULT 'f',
    time_created    bigint                    NOT NULL,
    time_modified   bigint                    NOT NULL,
    actor_created   text                      NOT NULL,
    actor_modified  text                      NOT NULL
);

CREATE UNIQUE INDEX module_str_name_idx ON module (str_name);

CREATE TABLE x_module_pipeline
(
    pk_x_module_pipeline uuid PRIMARY KEY,
    pk_module            uuid NOT NULL REFERENCES module (pk_module),
    pk_pipeline          uuid NOT NULL REFERENCES pipeline (pk_pipeline) ON DELETE CASCADE
);

CREATE UNIQUE INDEX x_module_pipeline_uniq_idx ON x_module_pipeline (pk_module, pk_pipeline);
CREATE INDEX x_module_pipeline_pk_pipeline_idx ON x_module_pipeline (pk_pipeline);

CREATE TABLE x_module_project
(
    pk_x_module_project  uuid PRIMARY KEY,
    pk_module            uuid NOT NULL REFERENCES module (pk_module),
    pk_project           uuid NOT NULL REFERENCES project (pk_project) ON DELETE CASCADE
);

CREATE UNIQUE INDEX x_module_project_uniq_idx ON x_module_project (pk_module, pk_project);
CREATE INDEX x_module_project_pk_project_idx ON x_module_project (pk_project);
