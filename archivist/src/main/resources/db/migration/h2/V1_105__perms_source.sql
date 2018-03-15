
ALTER TABLE user_permission ADD COLUMN str_source VARCHAR(128) NOT NULL DEFAULT 'local';
ALTER TABLE permission ADD COLUMN str_source VARCHAR(128) NOT NULL DEFAULT 'local';
