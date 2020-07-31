
ALTER TABLE project ADD COLUMN pk_pipeline_default UUID REFERENCES pipeline (pk_pipeline) ON DELETE SET NULL;
ALTER TABLE project ADD COLUMN pk_index_route UUID REFERENCES index_route (pk_index_route) ON DELETE SET NULL;

UPDATE project SET pk_pipeline_default =
    (SELECT pk_pipeline_default FROM project_settings WHERE project.pk_project = project_settings.pk_project);
UPDATE project SET pk_index_route =
    (SELECT pk_index_route_default FROM project_settings WHERE project.pk_project = project_settings.pk_project);

DROP TABLE project_settings;


---

CREATE TABLE index_task (
    pk_index_task UUID PRIMARY KEY,
    pk_project UUID NOT NULL REFERENCES project(pk_project) ON DELETE CASCADE ,
    pk_index_route_src UUID NOT NULL REFERENCES index_route(pk_index_route) ON DELETE CASCADE,
    pk_index_route_dst UUID REFERENCES index_route(pk_index_route) ON DELETE CASCADE,
    int_type SMALLINT NOT NULL DEFAULT 0,
    int_state SMALLINT NOT NULL DEFAULT 0,
    str_name TEXT NOT NULL,
    str_es_task_id TEXT NOT NULL,
    time_created BIGINT NOT NULL,
    time_modified BIGINT NOT NULL,
    actor_created TEXT NOT NULL,
    actor_modified TEXT NOT NULL
);

CREATE INDEX index_task_pk_project_idx ON index_task (pk_project);
CREATE INDEX index_task_pk_index_route_src_idx ON index_task (pk_index_route_src);
CREATE INDEX index_task_pk_index_route_dst_idx ON index_task (pk_index_route_dst);
CREATE INDEX index_task_int_state_idx ON index_task (int_state);
