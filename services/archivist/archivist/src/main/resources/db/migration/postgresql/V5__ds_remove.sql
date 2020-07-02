
--- Add a column for the module name which people can override.
--- Set the default to the current name.
ALTER TABLE model ADD COLUMN str_module TEXT;
UPDATE model SET str_module=str_name;
ALTER TABLE model ALTER COLUMN str_module SET NOT NULL;

CREATE UNIQUE INDEX model_str_module_uniq_idx ON model (str_module, pk_project);

ALTER TABLE model DROP COLUMN pk_data_set;
DROP TABLE data_set;



