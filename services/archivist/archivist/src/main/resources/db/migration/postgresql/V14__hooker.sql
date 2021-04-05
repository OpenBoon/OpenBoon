
CREATE TABLE webhook(
    pk_webhook UUID PRIMARY KEY,
    pk_project UUID NOT NULL REFERENCES project(pk_project) ON DELETE CASCADE,
    url TEXT NOT NULL,
    secret_token TEXT NOT NULL,
    triggers TEXT NOT NULL,
    active BOOLEAN NOT NULL,
    time_created BIGINT NOT NULL,
    time_modified BIGINT NOT NULL,
    actor_created TEXT NOT NULL,
    actor_modified TEXT NOT NULL
);

CREATE INDEX webhook_pk_project_idx ON webhook (pk_project);
