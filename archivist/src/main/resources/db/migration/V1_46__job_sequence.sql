
CREATE SEQUENCE JOB_SEQ;
ALTER TABLE job ALTER COLUMN pk_job BIGINT NOT NULL;

ALTER TABLE task DROP COLUMN str_execute;
