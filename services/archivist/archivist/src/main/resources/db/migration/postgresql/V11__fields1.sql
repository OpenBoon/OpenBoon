
CREATE TABLE field (
    pk_field UUID PRIMARY KEY,
    pk_project UUID NOT NULL REFERENCES project(pk_project) ON DELETE CASCADE,
    str_name VARCHAR(128) NOT NULL,
    str_type TEXT NOT NULL,
    time_created BIGINT NOT NULL,
    time_modified BIGINT NOT NULL,
    actor_created TEXT NOT NULL,
    actor_modified TEXT NOT NULL
);

CREATE UNIQUE INDEX field_unique_idx ON field (pk_project, str_name);