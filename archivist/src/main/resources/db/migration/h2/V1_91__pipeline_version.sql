
ALTER TABLE pipeline ADD COLUMN json_processors2 VARCHAR(2097152);
UPDATE pipeline SET json_processors2=json_processors;
ALTER TABLE pipeline DROP COLUMN json_processors;
ALTER TABLE pipeline ALTER COLUMN json_processors2 RENAME TO json_processors;
ALTER TABLE pipeline ALTER COLUMN json_processors SET DEFAULT '[]';
ALTER TABLE pipeline ALTER COLUMN json_processors SET NOT NULL;

ALTER TABLE pipeline ADD COLUMN int_version INTEGER NOT NULL DEFAULT 1;


