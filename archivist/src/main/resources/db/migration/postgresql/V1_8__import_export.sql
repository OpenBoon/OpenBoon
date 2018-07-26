
--- Delete imports
DELETE FROM job WHERE int_type=0;

ALTER TABLE job RENAME TO export;
ALTER TABLE export RENAME COLUMN pk_job TO pk_export;
ALTER TABLE export RENAME COLUMN time_started TO time_created;

--- Drop all this stuff
ALTER TABLE export DROP COLUMN int_type;
ALTER TABLE export DROP COLUMN str_root_path;

--- The job ID that it gets from the analyst.
ALTER TABLE export ADD COLUMN job_id UUID;

--- Add the org
ALTER TABLE export ADD COLUMN pk_organization UUID;
UPDATE export SET pk_organization='00000000-9998-8888-7777-666666666666';
ALTER TABLE export ALTER COLUMN pk_organization SET NOT NULL;
CREATE INDEX export_pk_organization_idx ON export (pk_organization);

--- no need to index by name I don't think
DROP INDEX job_name_idx;

--- export file changes.
ALTER TABLE export_file RENAME COLUMN pk_job TO pk_export;


DROP INDEX export_file_pk_job_idx;
CREATE UNIQUE INDEX export_file_pk_export_str_name_uidx ON export_file(pk_export, str_name);

