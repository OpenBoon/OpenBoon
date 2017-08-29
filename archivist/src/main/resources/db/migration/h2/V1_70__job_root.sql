ALTER TABLE job DROP COLUMN str_log_path;

ALTER TABLE job ADD str_root_path VARCHAR(255) NOT NULL;

