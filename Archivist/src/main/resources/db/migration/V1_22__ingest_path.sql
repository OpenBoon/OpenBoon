
ALTER TABLE ingest ADD json_paths TEXT NOT NULL;
UPDATE ingest SET json_paths='["' || str_path || '"]';
ALTER TABLE ingest DROP COLUMN str_path;

/*
 * A label for the ingest
 */
ALTER TABLE ingest ADD str_name VARCHAR(128) NOT NULL;

