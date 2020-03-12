
DROP INDEX datasource_str_name_idx;

CREATE UNIQUE INDEX datasource_pk_project_str_name_idx ON datasource USING btree (pk_project, str_name);
