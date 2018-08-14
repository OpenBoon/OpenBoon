DROP TABLE command;

ALTER TABLE users ADD COLUMN json_auth_attrs TEXT NOT NULL DEFAULT '{}';

