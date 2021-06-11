DROP TABLE automl;
DROP TABLE processor;
DROP TABLE plugin;

ALTER TABLE model ADD COLUMN json_exec_args JSONB NOT NULL DEFAULT '{}';
ALTER TABLE model ADD COLUMN str_endpoint TEXT;
