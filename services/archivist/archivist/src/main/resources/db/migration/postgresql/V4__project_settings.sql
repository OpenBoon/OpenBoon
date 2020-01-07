
CREATE TABLE project_settings (
    pk_project_settings      uuid PRIMARY KEY,
    pk_project               uuid               NOT NULL REFERENCES project(pk_project) ON DELETE CASCADE,
    pk_pipeline_default      uuid               NOT NULL REFERENCES pipeline(pk_pipeline),
    pk_index_route_default   uuid               NOT NULL REFERENCES index_route(pk_index_route)

);

CREATE INDEX project_settings_pk_pipeline_default_uniq_idx ON project_settings (pk_pipeline_default);
CREATE INDEX project_settings_pk_index_route_default_uniq_idx ON project_settings (pk_index_route_default);

---

DROP INDEX index_route_project_state_idx_uniq;
