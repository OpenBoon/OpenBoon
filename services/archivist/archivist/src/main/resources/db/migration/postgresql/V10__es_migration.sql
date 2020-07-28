
ALTER TABLE project ADD COLUMN pk_pipeline_default UUID REFERENCES pipeline (pk_pipeline) ON DELETE SET NULL;
ALTER TABLE project ADD COLUMN pk_index_route UUID REFERENCES index_route (pk_index_route) ON DELETE SET NULL;

UPDATE project SET pk_pipeline_default =
    (SELECT pk_pipeline_default FROM project_settings WHERE project.pk_project = project_settings.pk_project);
UPDATE project SET pk_index_route =
    (SELECT pk_index_route_default FROM project_settings WHERE project.pk_project = project_settings.pk_project);

DROP TABLE project_settings;