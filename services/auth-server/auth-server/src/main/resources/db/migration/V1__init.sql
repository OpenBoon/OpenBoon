CREATE TABLE api_key
(
    pk_api_key      UUID PRIMARY KEY,
    project_id      UUID NOT NULL,
    name            VARCHAR(128) NOT NULL,
    access_key      TEXT NOT NULL,
    secret_key      TEXT NOT NULL,
    permissions     TEXT NOT NULL,
    time_created    BIGINT NOT NULL,
    time_modified   BIGINT NOT NULL,
    actor_created   TEXT NOT NULL,
    actor_modified  TEXT NOT NULL
);

CREATE UNIQUE INDEX api_key_project_id_idx ON api_key (project_id, name);
CREATE UNIQUE INDEX api_key_access_key_uniq_idx ON api_key (access_key);