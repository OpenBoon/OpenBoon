DROP TABLE automl;

ALTER TABLE model ADD COLUMN json_exec_args JSONB NOT NULL DEFAULT '{}';
ALTER TABLE model ADD COLUMN str_endpoint TEXT;
