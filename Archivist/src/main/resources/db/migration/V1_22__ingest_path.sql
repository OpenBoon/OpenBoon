
ALTER TABLE ingest ADD json_paths TEXT;
UPDATE ingest SET json_paths='["' || str_path || '"]';
ALTER TABLE ingest ALTER COLUMN json_paths SET NOT NULL;

/*
 * A label for the ingest
 */
ALTER TABLE ingest ADD str_name VARCHAR(128);
UPDATE ingest SET str_name=str_path;
ALTER TABLE ingest ALTER COLUMN str_name SET NOT NULL;

ALTER TABLE ingest DROP COLUMN str_path;

