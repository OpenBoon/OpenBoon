CREATE TABLE api_key
(
    pk_api_key  UUID PRIMARY KEY,
    project_id  UUID NOT NULL,
    name        TEXT NOT NULL,
    shared_key  TEXT NOT NULL,
    permissions TEXT NOT NULL
);

CREATE UNIQUE INDEX api_key_project_id_idx ON api_key (project_id, name);