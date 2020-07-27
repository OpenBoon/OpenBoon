DROP INDEX model_str_module_uniq_idx;
CREATE UNIQUE INDEX model_str_module_uniq_idx ON model (str_name, pk_project);
