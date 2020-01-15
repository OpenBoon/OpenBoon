
ALTER TABLE datasource ADD COLUMN pk_pipeline UUID;
UPDATE datasource SET pk_pipeline =
    (SELECT ps.pk_pipeline_default FROM project_settings ps WHERE datasource.pk_project =  ps.pk_project);
ALTER TABLE datasource ALTER COLUMN pk_pipeline SET NOT NULL;