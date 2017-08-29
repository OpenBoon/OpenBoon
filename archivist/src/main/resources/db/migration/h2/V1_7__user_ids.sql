ALTER TABLE ingest DROP COLUMN str_user_created;
ALTER TABLE ingest DROP COLUMN str_user_modified;
ALTER TABLE ingest ADD user_created INT NOT NULL DEFAULT(0);
ALTER TABLE ingest ADD user_modified INT NOT NULL DEFAULT(0);

ALTER TABLE pipeline DROP COLUMN str_user_created;
ALTER TABLE pipeline DROP COLUMN str_user_modified;
ALTER TABLE pipeline ADD user_created INT NOT NULL DEFAULT(0);
ALTER TABLE pipeline ADD user_modified INT NOT NULL DEFAULT(0);

ALTER TABLE export DROP str_user_created;
ALTER TABLE export ADD user_created INT NOT NULL DEFAULT(0);

ALTER TABLE export_output DROP str_user_created;
ALTER TABLE export_output ADD user_created INT NOT NULL DEFAULT(0);

ALTER TABLE schedule DROP str_user_created;
ALTER TABLE schedule DROP str_user_modified;
ALTER TABLE schedule ADD user_created INT NOT NULL DEFAULT(0);
ALTER TABLE schedule ADD user_modified INT NOT NULL DEFAULT(0);
