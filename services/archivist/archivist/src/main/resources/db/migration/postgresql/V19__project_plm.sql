DROP TABLE x_module_project;

ALTER TABLE module ADD COLUMN pk_project UUID REFERENCES project(pk_project) ON DELETE CASCADE;
ALTER TABLE module DROP COLUMN bool_restricted;
ALTER TABLE module DROP COLUMN bool_standard;

DROP INDEX module_str_name_idx;
CREATE UNIQUE INDEX module_uniq_idx1 ON module ((pk_project IS NULL), str_name) WHERE pk_project IS NULL;
CREATE UNIQUE INDEX module_uniq_idx2 ON module (pk_project, str_name);
CREATE INDEX module_str_name_idx ON module (str_name);